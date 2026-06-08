package org.ruoyi.service.chat.impl.agent;

import cn.dev33.satoken.stp.StpUtil;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.skills.shell.ShellSkills;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.agent.ChartGenerationAgent;
import org.ruoyi.agent.CrmQueryAgent;
import org.ruoyi.agent.EchartsAgent;
import org.ruoyi.agent.SkillsAgent;
import org.ruoyi.agent.SqlAgent;
import org.ruoyi.agent.WebSearchAgent;
import org.ruoyi.agent.XtpServiceAgent;
import org.ruoyi.agent.tool.CrmQueryTool;
import org.ruoyi.agent.tool.ExecuteSqlQueryTool;
import org.ruoyi.agent.tool.QueryAllTablesTool;
import org.ruoyi.agent.tool.QueryTableSchemaTool;
import org.ruoyi.agent.tool.XtpManufacturingFlowTool;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.enums.RoleType;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.ruoyi.common.core.utils.SpringUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.common.sse.core.SseEmitterManager;
import org.ruoyi.common.sse.utils.SseMessageUtils;
import org.ruoyi.observability.MyAgentListener;
import org.ruoyi.observability.MyMcpClientListener;
import org.ruoyi.service.chat.IChatMessageService;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Agent runtime for the internal unified assistant.
 *
 * <p>Keeps the heavy multi-agent orchestration out of {@code ChatServiceFacade} and
 * avoids starting slow external MCP processes unless the current request needs them.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentRuntimeService {

    private static final int MAX_CONTEXT_MESSAGES = 12;

    private static final String DEFAULT_NPX_COMMAND = "/Users/yuejin/.nvm/versions/node/v24.12.0/bin/npx";

    private static final String NPX_COMMAND = System.getProperty("mcp.npx.command",
        System.getenv().getOrDefault("MCP_NPX_COMMAND", DEFAULT_NPX_COMMAND));

    private final IChatModelService chatModelService;

    private final SseEmitterManager sseEmitterManager;

    private final IChatMessageService chatMessageService;

    public SseEmitter agentChat(String assistantCode, ChatRequest chatRequest) {
        Long userId = LoginHelper.getUserId();
        String tokenValue = StpUtil.getTokenValue();
        SseEmitter emitter = sseEmitterManager.connect(userId, tokenValue);

        if (!"internal-unified".equals(assistantCode)) {
            sendRejectedAgentRun(userId, tokenValue, assistantCode, "不支持的助手: " + assistantCode);
            return emitter;
        }

        ChatModelVo chatModelVo = chatModelService.selectModelByName(chatRequest.getModel());
        if (chatModelVo == null) {
            sendRejectedAgentRun(userId, tokenValue, assistantCode, "模型不存在: " + chatRequest.getModel());
            return emitter;
        }

        chatRequest.setEnableThinking(true);
        chatRequest.setEmitter(emitter);
        chatRequest.setUserId(userId);
        chatRequest.setTokenValue(tokenValue);
        chatRequest.setChatModelVo(chatModelVo);
        chatRequest.setContextMessages(loadContextMessages(chatRequest));
        saveChatMessage(userId, chatRequest.getSessionId(), chatRequest.getContent(), RoleType.USER.getName(), chatRequest.getModel());

        return run(assistantCode, chatRequest);
    }

    public SseEmitter run(String assistantCode, ChatRequest chatRequest) {
        Long userId = chatRequest.getUserId();
        String tokenValue = chatRequest.getTokenValue();
        List<McpClient> mcpClients = new ArrayList<>();
        String runId = UUID.randomUUID().toString();
        AgentRuntimeProfile profile = AgentRuntimeProfile.from(chatRequest.getContent());

        sendAgentTrace(userId, "agent_run_start", Map.of(
            "runId", runId,
            "assistantCode", assistantCode,
            "title", "内部统一入口 Agent",
            "status", "running"
        ));

        try {
            OpenAiChatModel plannerModel = OpenAiChatModel.builder()
                .baseUrl(chatRequest.getChatModelVo().getApiHost())
                .apiKey(chatRequest.getChatModelVo().getApiKey())
                .modelName(chatRequest.getChatModelVo().getModelName())
                .build();

            java.nio.file.Path projectRoot = resolveProjectRoot();
            List<Object> subAgents = new ArrayList<>();

            addBusinessAgents(subAgents, plannerModel, userId, runId);
            addOptionalAgents(subAgents, plannerModel, profile, projectRoot, mcpClients, userId, runId);

            sendAgentTrace(userId, "agent_plan", Map.of(
                "runId", runId,
                "steps", profile.planSteps()
            ));

            SupervisorAgent supervisor = AgenticServices.supervisorBuilder()
                .chatModel(plannerModel)
                .supervisorContext(buildSupervisorContext(profile))
                .subAgents(subAgents.toArray())
                .responseStrategy(SupervisorResponseStrategy.LAST)
                .build();

            String supervisorInput = buildAgentConversationInput(chatRequest);

            CompletableFuture.runAsync(() -> executeSupervisor(
                supervisor,
                supervisorInput,
                assistantCode,
                chatRequest,
                userId,
                tokenValue,
                runId,
                mcpClients
            ));
            return chatRequest.getEmitter();
        } catch (Exception e) {
            String errorMessage = resolveErrorMessage(e);
            log.error("Agent runtime 初始化失败: {}", errorMessage, e);
            closeMcpClientsQuietly(mcpClients);
            sendAgentTrace(userId, "agent_run_done", Map.of(
                "runId", runId,
                "assistantCode", assistantCode,
                "status", "error",
                "error", errorMessage
            ));
            SseMessageUtils.sendError(userId, errorMessage);
            SseMessageUtils.completeConnection(userId, tokenValue);
            return chatRequest.getEmitter();
        }
    }

    private void executeSupervisor(SupervisorAgent supervisor, String supervisorInput, String assistantCode,
                                   ChatRequest chatRequest, Long userId, String tokenValue, String runId,
                                   List<McpClient> mcpClients) {
        try {
            sendAgentTrace(userId, "agent_step_start", tracePayload(runId, "supervisor", "Supervisor 编排", "AGENT", "running", null));
            String result = supervisor.invoke(supervisorInput);
            sendAgentTrace(userId, "agent_step_done", tracePayload(runId, "supervisor", "Supervisor 编排", "AGENT", "success", "已完成多 Agent 协作编排"));
            SseMessageUtils.sendContent(userId, result);
            saveChatMessage(userId, chatRequest.getSessionId(), result, RoleType.ASSISTANT.getName(), chatRequest.getModel());
            sendAgentTrace(userId, "agent_run_done", Map.of(
                "runId", runId,
                "assistantCode", assistantCode,
                "status", "success"
            ));
            SseMessageUtils.sendDone(userId);
        } catch (Exception e) {
            String errorMessage = resolveErrorMessage(e);
            log.error("Supervisor 执行失败", e);
            sendAgentTrace(userId, "agent_step_done", tracePayload(runId, "supervisor", "Supervisor 编排", "AGENT", "error", errorMessage));
            sendAgentTrace(userId, "agent_run_done", Map.of(
                "runId", runId,
                "assistantCode", assistantCode,
                "status", "error"
            ));
            SseMessageUtils.sendError(userId, errorMessage);
        } finally {
            closeMcpClientsQuietly(mcpClients);
            SseMessageUtils.completeConnection(userId, tokenValue);
        }
    }

    private void addBusinessAgents(List<Object> subAgents, OpenAiChatModel plannerModel, Long userId, String runId) {
        XtpManufacturingFlowTool xtpTool = SpringUtils.getBean(XtpManufacturingFlowTool.class);
        XtpServiceAgent xtpAgent = AgenticServices.agentBuilder(XtpServiceAgent.class)
            .chatModel(plannerModel)
            .tools(xtpTool)
            .listener(new MyAgentListener(userId, runId))
            .build();

        CrmQueryTool crmQueryTool = SpringUtils.getBean(CrmQueryTool.class);
        CrmQueryAgent crmQueryAgent = AgenticServices.agentBuilder(CrmQueryAgent.class)
            .chatModel(plannerModel)
            .tools(crmQueryTool)
            .listener(new MyAgentListener(userId, runId))
            .build();

        subAgents.add(crmQueryAgent);
        subAgents.add(xtpAgent);
    }

    private void addOptionalAgents(List<Object> subAgents, OpenAiChatModel plannerModel, AgentRuntimeProfile profile,
                                   java.nio.file.Path projectRoot, List<McpClient> mcpClients, Long userId, String runId) {
        if (profile.needsSql()) {
            SqlAgent sqlAgent = AgenticServices.agentBuilder(SqlAgent.class)
                .chatModel(plannerModel)
                .tools(new QueryAllTablesTool(), new QueryTableSchemaTool(), new ExecuteSqlQueryTool())
                .listener(new MyAgentListener(userId, runId))
                .build();
            subAgents.add(sqlAgent);
        }

        if (profile.needsChart()) {
            ChartGenerationAgent chartGenerationAgent = AgenticServices.agentBuilder(ChartGenerationAgent.class)
                .chatModel(plannerModel)
                .listener(new MyAgentListener(userId, runId))
                .build();
            EchartsAgent echartsAgent = AgenticServices.agentBuilder(EchartsAgent.class)
                .chatModel(plannerModel)
                .tools(new QueryAllTablesTool(), new QueryTableSchemaTool(), new ExecuteSqlQueryTool())
                .listener(new MyAgentListener(userId, runId))
                .build();
            subAgents.add(chartGenerationAgent);
            subAgents.add(echartsAgent);
        }

        if (profile.needsSkills()) {
            java.nio.file.Path skillsPath = projectRoot.resolve("ruoyi-admin/src/main/resources/skills");
            List<dev.langchain4j.skills.FileSystemSkill> skillsList = dev.langchain4j.skills.FileSystemSkillLoader
                .loadSkills(skillsPath);
            ShellSkills skills = ShellSkills.from(skillsList);
            SkillsAgent skillsAgent = AgenticServices.agentBuilder(SkillsAgent.class)
                .chatModel(plannerModel)
                .systemMessage("You have access to the following skills:\n" + skills.formatAvailableSkills()
                    + "\nWhen the user's request relates to one of these skills, activate it first using the `activate_skill` tool before proceeding.")
                .toolProvider(skills.toolProvider())
                .listener(new MyAgentListener(userId, runId))
                .build();
            subAgents.add(skillsAgent);
        }

        if (profile.needsExternalMcp()) {
            addMcpClientIfHealthy(mcpClients, "playwright",
                List.of(NPX_COMMAND, "-y", "@playwright/mcp@latest"), userId, runId);
            addMcpClientIfHealthy(mcpClients, "filesystem",
                List.of(NPX_COMMAND, "-y", "@modelcontextprotocol/server-filesystem", projectRoot.toString()), userId, runId);

            if (!mcpClients.isEmpty()) {
                ToolProvider toolProvider = McpToolProvider.builder()
                    .mcpClients(mcpClients)
                    .build();
                WebSearchAgent searchAgent = AgenticServices.agentBuilder(WebSearchAgent.class)
                    .chatModel(plannerModel)
                    .toolProvider(toolProvider)
                    .listener(new MyAgentListener(userId, runId))
                    .build();
                subAgents.add(searchAgent);
            }
        }
    }

    private String buildSupervisorContext(AgentRuntimeProfile profile) {
        StringBuilder context = new StringBuilder("""
            你是子 Agent 路由器，只能调用当前已注册的子 Agent 名称，不要创造未注册的 Agent 名。
            CRM 业务查询统一交给 crmQuery，包括 CRM 合同、客户、联系人、商机、报价、回款计划。
            XTP 制造、工单、采购、库存等制造闭环查询交给 xtpQuery。
            优先使用 CRM/XTP 专用 Agent，不要为了业务表查询直接走通用 SQL。
            """);
        if (profile.needsSql()) {
            context.append("数据库通用查询可交给 sqlQuery。\n");
        }
        if (profile.needsSkills()) {
            context.append("文档处理交给 skills。\n");
        }
        if (profile.needsExternalMcp()) {
            context.append("网页搜索、浏览器操作或文件系统访问交给 webSearch。\n");
        }
        if (profile.needsChart()) {
            context.append("图表配置生成交给 chartGeneration 或 echarts。\n");
        }
        return context.toString();
    }

    private List<ChatMessage> loadContextMessages(ChatRequest chatRequest) {
        if (chatRequest.getSessionId() == null) {
            return new ArrayList<>();
        }
        List<ChatMessage> messages = chatMessageService.getMessagesBySessionId(chatRequest.getSessionId());
        if (messages == null || messages.size() <= MAX_CONTEXT_MESSAGES) {
            return messages == null ? new ArrayList<>() : messages;
        }
        return new ArrayList<>(messages.subList(messages.size() - MAX_CONTEXT_MESSAGES, messages.size()));
    }

    private String buildAgentConversationInput(ChatRequest chatRequest) {
        List<ChatMessage> contextMessages = chatRequest.getContextMessages();
        if (contextMessages == null || contextMessages.isEmpty()) {
            return chatRequest.getContent();
        }

        StringBuilder input = new StringBuilder();
        input.append("以下是同一会话的历史上下文，请在回答当前问题时继承上一轮对话结果。\n");
        input.append("如果当前问题使用了“它、这个、上面、刚才、继续、再查”等指代，请优先从历史上下文中解析。\n\n");
        input.append("【会话上下文】\n");

        for (ChatMessage message : contextMessages) {
            String role = resolveMessageRole(message);
            String text = resolveMessageText(message);
            if (StringUtils.isBlank(text)) {
                continue;
            }
            input.append(role).append(": ").append(text).append("\n");
        }

        input.append("\n【当前用户问题】\n").append(chatRequest.getContent());
        return input.toString();
    }

    private String resolveMessageRole(ChatMessage message) {
        if (message instanceof UserMessage) {
            return "用户";
        }
        if (message instanceof AiMessage) {
            return "助手";
        }
        return message.type() != null ? message.type().name() : "消息";
    }

    private String resolveMessageText(ChatMessage message) {
        if (message instanceof UserMessage userMessage) {
            return userMessage.singleText();
        }
        if (message instanceof AiMessage aiMessage) {
            return aiMessage.text();
        }
        return message.toString();
    }

    private void sendRejectedAgentRun(Long userId, String tokenValue, String assistantCode, String errorMessage) {
        String runId = UUID.randomUUID().toString();
        sendAgentTrace(userId, "agent_run_start", Map.of(
            "runId", runId,
            "assistantCode", assistantCode,
            "title", "内部统一入口 Agent",
            "status", "running"
        ));
        sendAgentTrace(userId, "agent_step_done", tracePayload(
            runId,
            "request-validation",
            "请求校验",
            "AGENT",
            "error",
            errorMessage
        ));
        sendAgentTrace(userId, "agent_run_done", Map.of(
            "runId", runId,
            "assistantCode", assistantCode,
            "title", "内部统一入口 Agent",
            "status", "error",
            "result", errorMessage
        ));
        SseMessageUtils.sendError(userId, errorMessage);
        SseMessageUtils.completeConnection(userId, tokenValue);
    }

    private void sendAgentTrace(Long userId, String event, Map<String, Object> payload) {
        SseMessageUtils.sendAgentEvent(userId, event, payload);
    }

    private Map<String, Object> tracePayload(String runId, String stepId, String name, String type, String status, Object result) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("runId", runId);
        payload.put("stepId", stepId);
        payload.put("name", name);
        payload.put("type", type);
        payload.put("status", status);
        payload.put("timestamp", System.currentTimeMillis());
        if (result != null) {
            payload.put("result", result);
        }
        return payload;
    }

    private void addMcpClientIfHealthy(List<McpClient> clients, String name, List<String> command, Long userId, String runId) {
        McpClient client = null;
        try {
            McpTransport transport = new StdioMcpTransport.Builder()
                .command(command)
                .logEvents(true)
                .build();

            client = new DefaultMcpClient.Builder()
                .transport(transport)
                .listener(new MyMcpClientListener(userId, runId))
                .build();

            client.listTools();
            clients.add(client);
            log.info("MCP 工具启动成功: {}, command={}", name, command);
        } catch (Exception e) {
            log.warn("跳过不可用 MCP 工具: {}, command={}, error={}", name, command, e.getMessage());
            if (client != null) {
                try {
                    client.close();
                } catch (Exception closeError) {
                    log.debug("关闭不可用 MCP 工具失败: {}, error={}", name, closeError.getMessage());
                }
            }
        }
    }

    private void closeMcpClientsQuietly(List<McpClient> clients) {
        for (McpClient client : clients) {
            try {
                client.close();
            } catch (Exception ignored) {
                // ignore close failures
            }
        }
    }

    private java.nio.file.Path resolveProjectRoot() {
        java.nio.file.Path cwd = java.nio.file.Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        if (cwd.endsWith("ruoyi-admin")) {
            return cwd.getParent();
        }
        return cwd;
    }

    private void saveChatMessage(Long userId, Long sessionId, String content, String role, String modelName) {
        chatMessageService.saveChatMessage(userId, sessionId, content, role, modelName);
    }

    private String resolveErrorMessage(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (StringUtils.isNotBlank(current.getMessage())) {
                return current.getMessage();
            }
            current = current.getCause();
        }
        return "模型调用失败，请检查模型名称、API Key、接口地址和账号权限";
    }

    private record AgentRuntimeProfile(boolean needsSql, boolean needsChart, boolean needsSkills, boolean needsExternalMcp) {

        static AgentRuntimeProfile from(String content) {
            String text = content == null ? "" : content.toLowerCase(Locale.ROOT);
            boolean needsChart = containsAny(text, "图表", "echarts", "chart", "柱状图", "折线图", "饼图", "可视化");
            boolean needsSkills = containsAny(text, "word", "docx", "pdf", "excel", "xlsx", "文档", "表格", "文件生成", "报告");
            boolean needsExternalMcp = containsAny(text, "浏览器", "网页", "网址", "http", "https", "搜索", "联网", "playwright", "文件系统", "目录", "读取文件");
            boolean needsSql = containsAny(text, "sql", "数据库", "数据表", "表结构", "select ", "查询表");
            return new AgentRuntimeProfile(needsSql, needsChart, needsSkills, needsExternalMcp);
        }

        List<String> planSteps() {
            List<String> steps = new ArrayList<>();
            steps.add("理解用户问题并选择业务子 Agent");
            steps.add("优先查询 CRM 客户/合同/联系人等业务信息");
            steps.add("必要时查询 XTP 制造、工单、阶段、采购、库存信息");
            if (needsSql) {
                steps.add("按需启用通用 SQL 查询能力");
            }
            if (needsChart) {
                steps.add("按需生成图表配置");
            }
            if (needsSkills) {
                steps.add("按需启用文档处理技能");
            }
            if (needsExternalMcp) {
                steps.add("按需启动外部 MCP 工具");
            }
            steps.add("汇总为内部可读业务答复");
            return steps;
        }

        private static boolean containsAny(String text, String... keywords) {
            for (String keyword : keywords) {
                if (text.contains(keyword)) {
                    return true;
                }
            }
            return false;
        }
    }
}
