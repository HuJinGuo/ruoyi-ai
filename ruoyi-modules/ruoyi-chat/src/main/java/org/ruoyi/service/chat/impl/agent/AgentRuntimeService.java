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
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.PartialResponse;
import dev.langchain4j.model.chat.response.PartialResponseContext;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.skills.shell.ShellSkills;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.agent.ChartGenerationAgent;
import org.ruoyi.agent.CrmBusinessAgent;
import org.ruoyi.agent.EchartsAgent;
import org.ruoyi.agent.EngineeringBusinessAgent;
import org.ruoyi.agent.MesBusinessAgent;
import org.ruoyi.agent.SkillsAgent;
import org.ruoyi.agent.SqlAgent;
import org.ruoyi.agent.SrmBusinessAgent;
import org.ruoyi.agent.WebSearchAgent;
import org.ruoyi.agent.WmsBusinessAgent;
import org.ruoyi.agent.XtpServiceAgent;
import org.ruoyi.agent.tool.CrmBusinessTool;
import org.ruoyi.agent.tool.EngineeringBusinessTool;
import org.ruoyi.agent.tool.ExecuteSqlQueryTool;
import org.ruoyi.agent.tool.MesBusinessTool;
import org.ruoyi.agent.tool.QueryAllTablesTool;
import org.ruoyi.agent.tool.QueryTableSchemaTool;
import org.ruoyi.agent.tool.SrmBusinessTool;
import org.ruoyi.agent.tool.WmsBusinessTool;
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
import org.ruoyi.factory.ChatServiceFactory;
import org.ruoyi.observability.AgentTraceContext;
import org.ruoyi.observability.MyAgentListener;
import org.ruoyi.observability.MyMcpClientListener;
import org.ruoyi.service.chat.AbstractChatService;
import org.ruoyi.service.chat.IChatMessageService;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

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

    private final ChatServiceFactory chatServiceFactory;

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
        SseEmitter emitter = chatRequest.getEmitter();
        List<McpClient> mcpClients = new ArrayList<>();
        String runId = UUID.randomUUID().toString();
        AgentRuntimeProfile profile = AgentRuntimeProfile.from(chatRequest.getContent());

        sendAgentTrace(emitter, "agent_run_start", Map.of(
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

            String supervisorInput = buildAgentConversationInput(chatRequest);
            sendAgentTrace(emitter, "agent_plan", Map.of(
                "runId", runId,
                "steps", profile.planSteps()
            ));
            if (profile.canRunDirectly()) {
                CompletableFuture.runAsync(() -> executeDirectAgent(
                    buildDirectBusinessAgent(profile, plannerModel, userId, runId, emitter),
                    supervisorInput,
                    assistantCode,
                    chatRequest,
                    userId,
                    tokenValue,
                    runId,
                    emitter,
                    mcpClients
                ));
                return chatRequest.getEmitter();
            }
            if (profile.canRunSequentially()) {
                CompletableFuture.runAsync(() -> executeSequentialBusinessAgents(
                    buildDirectBusinessAgents(profile, plannerModel, userId, runId, emitter),
                    supervisorInput,
                    assistantCode,
                    chatRequest,
                    userId,
                    tokenValue,
                    runId,
                    emitter,
                    mcpClients
                ));
                return chatRequest.getEmitter();
            }

            addBusinessAgents(subAgents, plannerModel, profile, userId, runId, emitter);
            addOptionalAgents(subAgents, plannerModel, profile, projectRoot, mcpClients, userId, runId, emitter);

            SupervisorAgent supervisor = AgenticServices.supervisorBuilder()
                .chatModel(plannerModel)
                .supervisorContext(buildSupervisorContext(profile))
                .subAgents(subAgents.toArray())
                .responseStrategy(SupervisorResponseStrategy.SUMMARY)
                .build();

            CompletableFuture.runAsync(() -> executeSupervisor(
                supervisor,
                supervisorInput,
                assistantCode,
                chatRequest,
                userId,
                tokenValue,
                runId,
                emitter,
                mcpClients
            ));
            return chatRequest.getEmitter();
        } catch (Exception e) {
            String errorMessage = resolveErrorMessage(e);
            log.error("Agent runtime 初始化失败: {}", errorMessage, e);
            closeMcpClientsQuietly(mcpClients);
            sendAgentTrace(emitter, "agent_run_done", Map.of(
                "runId", runId,
                "assistantCode", assistantCode,
                "status", "error",
                "error", errorMessage
            ));
            SseMessageUtils.sendError(emitter, errorMessage);
            SseMessageUtils.completeConnection(userId, tokenValue);
            return chatRequest.getEmitter();
        }
    }

    private Function<String, String> buildDirectBusinessAgent(AgentRuntimeProfile profile, OpenAiChatModel plannerModel,
                                                             Long userId, String runId, SseEmitter emitter) {
        BusinessDomain domain = profile.singleBusinessDomain();
        if (domain == null) {
            domain = BusinessDomain.CRM;
        }
        return buildDirectAgent(domain, plannerModel, userId, runId, emitter);
    }

    private List<BusinessAgentInvoker> buildDirectBusinessAgents(AgentRuntimeProfile profile, OpenAiChatModel plannerModel,
                                                                 Long userId, String runId, SseEmitter emitter) {
        List<BusinessAgentInvoker> invokers = new ArrayList<>();
        for (BusinessDomain domain : profile.orderedBusinessDomains()) {
            invokers.add(new BusinessAgentInvoker(domain, buildDirectAgent(domain, plannerModel, userId, runId, emitter)));
        }
        return invokers;
    }

    private void executeDirectAgent(Function<String, String> directAgent, String input, String assistantCode,
                                    ChatRequest chatRequest, Long userId, String tokenValue, String runId,
                                    SseEmitter emitter, List<McpClient> mcpClients) {
        try {
            AgentTraceContext.set(userId, runId, emitter);
            sendAgentTrace(emitter, "agent_step_start", tracePayload(runId, "direct-agent", "单业务域直连", "AGENT", "running", null));
            String result = directAgent.apply(input);
            sendAgentTrace(emitter, "agent_step_done", tracePayload(runId, "direct-agent", "单业务域直连", "AGENT", "success", "已跳过 Supervisor，直连完成"));
            streamContent(emitter, result);
            saveChatMessage(userId, chatRequest.getSessionId(), result, RoleType.ASSISTANT.getName(), chatRequest.getModel());
            sendAgentTrace(emitter, "agent_run_done", Map.of(
                "runId", runId,
                "assistantCode", assistantCode,
                "status", "success",
                "mode", "direct"
            ));
            SseMessageUtils.sendDone(emitter);
        } catch (Exception e) {
            String errorMessage = resolveErrorMessage(e);
            log.error("Direct Agent 执行失败", e);
            sendAgentTrace(emitter, "agent_step_done", tracePayload(runId, "direct-agent", "单业务域直连", "AGENT", "error", errorMessage));
            sendAgentTrace(emitter, "agent_run_done", Map.of(
                "runId", runId,
                "assistantCode", assistantCode,
                "status", "error",
                "mode", "direct"
            ));
            SseMessageUtils.sendError(emitter, errorMessage);
        } finally {
            AgentTraceContext.clear();
            closeMcpClientsQuietly(mcpClients);
            SseMessageUtils.completeConnection(userId, tokenValue);
        }
    }

    private void executeSequentialBusinessAgents(List<BusinessAgentInvoker> invokers, String input, String assistantCode,
                                                 ChatRequest chatRequest, Long userId, String tokenValue, String runId,
                                                 SseEmitter emitter, List<McpClient> mcpClients) {
        List<BusinessAgentResult> results = new ArrayList<>();
        try {
            AgentTraceContext.set(userId, runId, emitter);
            for (BusinessAgentInvoker invoker : invokers) {
                String stepId = "business-" + invoker.domain().name().toLowerCase(Locale.ROOT);
                sendAgentTrace(emitter, "agent_step_start", tracePayload(runId, stepId, invoker.domain().displayName(), "AGENT", "running", null));
                String stepInput = buildSequentialStepInput(input, results);
                String output = invoker.invoke(stepInput);
                results.add(new BusinessAgentResult(invoker.domain(), output));
                sendAgentTrace(emitter, "agent_step_done", tracePayload(runId, stepId, invoker.domain().displayName(), "AGENT", "success", truncate(output, 800)));
            }

            sendAgentTrace(emitter, "agent_step_start", tracePayload(runId, "business-final-synthesis", "综合分析整理", "MODEL", "running", null));
            String result = streamFinalSynthesis(chatRequest, input, results, emitter);
            sendAgentTrace(emitter, "agent_step_done", tracePayload(runId, "business-final-synthesis", "综合分析整理", "MODEL", "success", "已根据用户问题完成综合整理"));
            saveChatMessage(userId, chatRequest.getSessionId(), result, RoleType.ASSISTANT.getName(), chatRequest.getModel());
            sendAgentTrace(emitter, "agent_run_done", Map.of(
                "runId", runId,
                "assistantCode", assistantCode,
                "status", "success",
                "mode", "sequential"
            ));
            SseMessageUtils.sendDone(emitter);
        } catch (Exception e) {
            String errorMessage = resolveErrorMessage(e);
            log.error("Sequential Business Agent 执行失败", e);
            sendAgentTrace(emitter, "agent_run_done", Map.of(
                "runId", runId,
                "assistantCode", assistantCode,
                "status", "error",
                "mode", "sequential"
            ));
            SseMessageUtils.sendError(emitter, errorMessage);
        } finally {
            AgentTraceContext.clear();
            closeMcpClientsQuietly(mcpClients);
            SseMessageUtils.completeConnection(userId, tokenValue);
        }
    }

    private String buildSequentialStepInput(String originalInput, List<BusinessAgentResult> previousResults) {
        if (previousResults.isEmpty()) {
            return originalInput;
        }
        StringBuilder input = new StringBuilder();
        input.append("【原始用户问题】\n").append(originalInput).append("\n\n");
        input.append("【前序业务 Agent 已查询到的信息】\n");
        for (BusinessAgentResult result : previousResults) {
            input.append("## ").append(result.domain().displayName()).append("\n")
                .append(result.output()).append("\n\n");
        }
        input.append("请只处理你负责的业务域；如果前序结果中已有可用编号、客户、合同、工单等线索，应优先使用这些线索继续查询。");
        input.append("最终输出必须是中文 Markdown 摘要，结构化字段必须表格化：单对象用“| 字段 | 内容 |”两列表格，多对象用业务列表表格；不要原样倾倒 JSON，不要输出连续的“字段：值”纯文本段落。");
        input.append("重点字段要标记：客户名、联系人、合同名、工单号、金额、状态、当前阶段、进度、交付日期使用 Markdown 加粗；缺料、异常、失败、逾期、风险、暂未查询到等负向信息使用 <mark>...</mark> 高亮。");
        return input.toString();
    }

    private String formatSequentialResults(List<BusinessAgentResult> results) {
        StringBuilder answer = new StringBuilder("# 综合查询结果\n\n");
        for (BusinessAgentResult result : results) {
            answer.append("## ").append(result.domain().displayName()).append("\n");
            answer.append(result.output()).append("\n\n");
        }
        answer.append("> 说明：以上按业务能力顺序查询；若某一部分提示缺少编号或暂未查询到，不影响其他已查询到的信息。");
        return answer.toString();
    }

    private String buildFinalSynthesisPrompt(String originalInput, List<BusinessAgentResult> results) {
        StringBuilder evidence = new StringBuilder();
        for (BusinessAgentResult result : results) {
            evidence.append("## ").append(result.domain().displayName()).append("\n")
                .append(result.output()).append("\n\n");
        }

        return """
            你是内部统一入口 Agent 的最终答复整理器。下面是用户原始问题和各业务子 Agent 已查询到的事实证据。

            【用户原始问题】
            %s

            【子 Agent 证据】
            %s

            请基于证据生成最终回答，要求：
            1. 不要按 CRM/MES/工程/SRM/WMS 这种系统名称机械分组，除非用户明确问某个系统。
            2. 优先围绕用户真实意图组织答案：如果用户问“怎么回复客户/话术”，先给可直接发送给客户的话术，再给内部依据；如果用户问“进度/情况”，先给一句结论，再按“当前状态、已完成、进行中、下一步、风险/注意事项”组织。
            3. 表格只在确实有助于比较或承载关键字段时使用；不要为了表格而表格。单对象信息优先用简短段落或要点，多条记录再用紧凑表格。
            4. 关键值使用 Markdown 加粗，例如客户名、合同名、工单号、金额、状态、当前阶段、进度、交付日期。
            5. 负向或风险信息使用 <mark>...</mark>，例如缺料、异常、失败、逾期、风险、暂未查询到。
            6. 不要输出裸 JSON、Java 异常、工具原文，也不要暴露“子 Agent 证据”字样。
            7. 事实必须来自证据，不要编造；证据缺失时明确说暂未查询到。
            8. 用中文 Markdown，版面紧凑，尽量少用大表格。
            """.formatted(originalInput, evidence);
    }

    private String streamFinalSynthesis(ChatRequest chatRequest, String originalInput, List<BusinessAgentResult> results,
                                        SseEmitter emitter) {
        String prompt = buildFinalSynthesisPrompt(originalInput, results);
        try {
            AbstractChatService chatService = chatServiceFactory.getOriginalService(chatRequest.getChatModelVo().getProviderCode());
            StreamingChatModel streamingChatModel = chatService.buildStreamingChatModel(chatRequest.getChatModelVo(), chatRequest);
            String answer = streamModelContent(streamingChatModel, prompt, emitter);
            return StringUtils.isBlank(answer) ? streamFallbackSequentialResults(results, emitter) : answer;
        } catch (Exception e) {
            log.warn("综合分析整理失败，回退为顺序结果拼接: {}", e.getMessage());
            return streamFallbackSequentialResults(results, emitter);
        }
    }

    private String streamModelContent(StreamingChatModel streamingChatModel, String prompt, SseEmitter emitter) {
        StringBuilder messageBuffer = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        streamingChatModel.chat(prompt, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                appendAndSend(partialResponse);
            }

            @Override
            public void onPartialResponse(PartialResponse partialResponse, PartialResponseContext context) {
                appendAndSend(partialResponse.text());
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                latch.countDown();
            }

            @Override
            public void onError(Throwable error) {
                errorRef.set(error);
                latch.countDown();
            }

            private void appendAndSend(String partialResponse) {
                if (StringUtils.isBlank(partialResponse)) {
                    return;
                }
                messageBuffer.append(partialResponse);
                SseMessageUtils.sendContent(emitter, partialResponse);
            }
        });

        awaitStreamingComplete(latch);
        Throwable error = errorRef.get();
        if (error != null) {
            throw new IllegalStateException(resolveErrorMessage(error), error);
        }
        return messageBuffer.toString();
    }

    private void awaitStreamingComplete(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("综合分析整理被中断", e);
        }
    }

    private String streamFallbackSequentialResults(List<BusinessAgentResult> results, SseEmitter emitter) {
        String fallback = formatSequentialResults(results);
        streamContent(emitter, fallback);
        return fallback;
    }

    private void executeSupervisor(SupervisorAgent supervisor, String supervisorInput, String assistantCode,
                                   ChatRequest chatRequest, Long userId, String tokenValue, String runId,
                                   SseEmitter emitter, List<McpClient> mcpClients) {
        try {
            AgentTraceContext.set(userId, runId, emitter);
            sendAgentTrace(emitter, "agent_step_start", tracePayload(runId, "supervisor", "Supervisor 编排", "AGENT", "running", null));
            String result = supervisor.invoke(supervisorInput);
            sendAgentTrace(emitter, "agent_step_done", tracePayload(runId, "supervisor", "Supervisor 编排", "AGENT", "success", "已完成多 Agent 协作编排"));
            streamContent(emitter, result);
            saveChatMessage(userId, chatRequest.getSessionId(), result, RoleType.ASSISTANT.getName(), chatRequest.getModel());
            sendAgentTrace(emitter, "agent_run_done", Map.of(
                "runId", runId,
                "assistantCode", assistantCode,
                "status", "success"
            ));
            SseMessageUtils.sendDone(emitter);
        } catch (Exception e) {
            String errorMessage = resolveErrorMessage(e);
            log.error("Supervisor 执行失败", e);
            sendAgentTrace(emitter, "agent_step_done", tracePayload(runId, "supervisor", "Supervisor 编排", "AGENT", "error", errorMessage));
            sendAgentTrace(emitter, "agent_run_done", Map.of(
                "runId", runId,
                "assistantCode", assistantCode,
                "status", "error"
            ));
            SseMessageUtils.sendError(emitter, errorMessage);
        } finally {
            AgentTraceContext.clear();
            closeMcpClientsQuietly(mcpClients);
            SseMessageUtils.completeConnection(userId, tokenValue);
        }
    }

    private void addBusinessAgents(List<Object> subAgents, OpenAiChatModel plannerModel, AgentRuntimeProfile profile,
                                   Long userId, String runId, SseEmitter emitter) {
        for (BusinessDomain domain : profile.businessDomains()) {
            subAgents.add(buildSubAgent(domain, plannerModel, userId, runId, emitter));
        }
        if (subAgents.isEmpty()) {
            subAgents.add(buildSubAgent(BusinessDomain.CRM, plannerModel, userId, runId, emitter));
        }
    }

    private Object buildSubAgent(BusinessDomain domain, OpenAiChatModel plannerModel, Long userId, String runId, SseEmitter emitter) {
        return switch (domain) {
            case CRM -> AgenticServices.agentBuilder(CrmBusinessAgent.class)
                .chatModel(plannerModel)
                .tools(SpringUtils.getBean(CrmBusinessTool.class))
                .listener(new MyAgentListener(userId, runId, emitter))
                .build();
            case MES -> AgenticServices.agentBuilder(MesBusinessAgent.class)
                .chatModel(plannerModel)
                .tools(SpringUtils.getBean(MesBusinessTool.class))
                .listener(new MyAgentListener(userId, runId, emitter))
                .build();
            case ENGINEERING -> AgenticServices.agentBuilder(EngineeringBusinessAgent.class)
                .chatModel(plannerModel)
                .tools(SpringUtils.getBean(EngineeringBusinessTool.class))
                .listener(new MyAgentListener(userId, runId, emitter))
                .build();
            case SRM -> AgenticServices.agentBuilder(SrmBusinessAgent.class)
                .chatModel(plannerModel)
                .tools(SpringUtils.getBean(SrmBusinessTool.class))
                .listener(new MyAgentListener(userId, runId, emitter))
                .build();
            case WMS -> AgenticServices.agentBuilder(WmsBusinessAgent.class)
                .chatModel(plannerModel)
                .tools(SpringUtils.getBean(WmsBusinessTool.class))
                .listener(new MyAgentListener(userId, runId, emitter))
                .build();
//            case XTP -> AgenticServices.agentBuilder(XtpServiceAgent.class)
//                .chatModel(plannerModel)
//                .tools(SpringUtils.getBean(XtpManufacturingFlowTool.class))
//                .listener(new MyAgentListener(userId, runId, emitter))
//                .build();
        };
    }

    private Function<String, String> buildDirectAgent(BusinessDomain domain, OpenAiChatModel plannerModel,
                                                      Long userId, String runId, SseEmitter emitter) {
        return switch (domain) {
            case CRM -> {
                CrmBusinessAgent agent = (CrmBusinessAgent) buildSubAgent(domain, plannerModel, userId, runId, emitter);
                yield agent::handle;
            }
            case MES -> {
                MesBusinessAgent agent = (MesBusinessAgent) buildSubAgent(domain, plannerModel, userId, runId, emitter);
                yield agent::handle;
            }
            case ENGINEERING -> {
                EngineeringBusinessAgent agent = (EngineeringBusinessAgent) buildSubAgent(domain, plannerModel, userId, runId, emitter);
                yield agent::handle;
            }
            case SRM -> {
                SrmBusinessAgent agent = (SrmBusinessAgent) buildSubAgent(domain, plannerModel, userId, runId, emitter);
                yield agent::handle;
            }
            case WMS -> {
                WmsBusinessAgent agent = (WmsBusinessAgent) buildSubAgent(domain, plannerModel, userId, runId, emitter);
                yield agent::handle;
            }
//            case XTP -> {
//                XtpServiceAgent agent = (XtpServiceAgent) buildSubAgent(domain, plannerModel, userId, runId, emitter);
//                yield agent::getData;
//            }
        };
    }

    private void addOptionalAgents(List<Object> subAgents, OpenAiChatModel plannerModel, AgentRuntimeProfile profile,
                                   java.nio.file.Path projectRoot, List<McpClient> mcpClients, Long userId, String runId, SseEmitter emitter) {
        if (profile.needsSql()) {
            SqlAgent sqlAgent = AgenticServices.agentBuilder(SqlAgent.class)
                .chatModel(plannerModel)
                .tools(new QueryAllTablesTool(), new QueryTableSchemaTool(), new ExecuteSqlQueryTool())
                .listener(new MyAgentListener(userId, runId, emitter))
                .build();
            subAgents.add(sqlAgent);
        }

        if (profile.needsChart()) {
            ChartGenerationAgent chartGenerationAgent = AgenticServices.agentBuilder(ChartGenerationAgent.class)
                .chatModel(plannerModel)
                .listener(new MyAgentListener(userId, runId, emitter))
                .build();
            EchartsAgent echartsAgent = AgenticServices.agentBuilder(EchartsAgent.class)
                .chatModel(plannerModel)
                .tools(new QueryAllTablesTool(), new QueryTableSchemaTool(), new ExecuteSqlQueryTool())
                .listener(new MyAgentListener(userId, runId, emitter))
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
                .listener(new MyAgentListener(userId, runId, emitter))
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
                    .listener(new MyAgentListener(userId, runId, emitter))
                    .build();
                subAgents.add(searchAgent);
            }
        }
    }

    private String buildSupervisorContext(AgentRuntimeProfile profile) {
        StringBuilder context = new StringBuilder("""
            你是子 Agent 路由器，只能调用当前已注册的子 Agent 名称，不要创造未注册的 Agent 名。
            涉及新增、更新、入库等写操作时，必须先生成草稿并等待用户明确确认；确认入库只能使用上一轮工具返回的 draftId，禁止自行编造 draftId。
            优先使用各系统业务 Agent，不要为了业务表查询直接走通用 SQL。
            涉及业务事实、字段名、客户名、合同名、编号、金额、状态、联系人等内容，必须严格来自子 agent 或工具返回结果。
            必须汇总所有已调用子 Agent 的有效结果，不能因为后续子 Agent 缺少参数或查不到数据，就覆盖或丢弃前面已经查到的信息。
            如果部分模块查不到，最终回答应包含“已查询到”和“暂未查询到/需要补充”的分区；已查询到的客户、合同、联系人、商机等信息必须先返回。
            当用户以客户/公司为入口询问合同、物料、工单进展、交付进度、制造闭环时，应按 CRM -> MES -> 工程物料 -> SRM -> WMS 逐步查询，不要要求用户先提供 MES 工单编号。
            最终回答必须使用中文 Markdown：用 #/## 标题分区，结构化字段必须表格化。

           
            禁止输出裸 JSON、Java 异常、未整理的大段工具原文；禁止把结构化字段写成连续的“字段：值”纯文本段落。

            """);
        for (BusinessDomain domain : profile.businessDomains()) {
            context.append(domain.supervisorHint()).append("\n");
        }
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

    private void sendAgentTrace(SseEmitter emitter, String event, Map<String, Object> payload) {
        SseMessageUtils.sendAgentEvent(emitter, event, payload);
    }

    private void streamContent(SseEmitter emitter, String content) {
        if (StringUtils.isBlank(content)) {
            return;
        }
        for (String chunk : splitStreamChunks(content)) {
            SseMessageUtils.sendContent(emitter, chunk);
            pauseBriefly();
        }
    }

    private List<String> splitStreamChunks(String content) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < content.length()) {
            int end = findChunkEnd(content, start);
            chunks.add(content.substring(start, end));
            start = end;
        }
        return chunks;
    }

    private int findChunkEnd(String content, int start) {
        int maxEnd = Math.min(content.length(), start + 96);
        if (maxEnd >= content.length()) {
            return content.length();
        }
        int paragraphEnd = content.indexOf("\n\n", start);
        if (paragraphEnd > start && paragraphEnd + 2 <= maxEnd) {
            return paragraphEnd + 2;
        }
        int lineEnd = content.indexOf('\n', start);
        if (lineEnd > start && lineEnd + 1 <= maxEnd) {
            return lineEnd + 1;
        }
        for (int i = maxEnd; i > start + 32; i--) {
            char ch = content.charAt(i - 1);
            if (ch == '。' || ch == '；' || ch == ';' || ch == '，' || ch == ',' || Character.isWhitespace(ch)) {
                return i;
            }
        }
        return maxEnd;
    }

    private void pauseBriefly() {
        try {
            Thread.sleep(18L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
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
                String message = current.getMessage();
                if (isUpstreamStreamInterrupted(message)) {
                    return "模型上游流式响应提前中断，未收到完整结束事件。请稍后重试，或切换到更稳定的模型/接口地址。";
                }
                return message;
            }
            current = current.getCause();
        }
        return "模型调用失败，请检查模型名称、API Key、接口地址和账号权限";
    }

    private boolean isUpstreamStreamInterrupted(String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("upstream stream ended without a terminal response event")
            || lower.contains("upstream request failed")
            || lower.contains("upstream_error");
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    private record BusinessAgentInvoker(BusinessDomain domain, Function<String, String> delegate) {
        String invoke(String input) {
            return delegate.apply(input);
        }
    }

    private record BusinessAgentResult(BusinessDomain domain, String output) {
    }

    private enum BusinessDomain {
        CRM(
            "CRM 业务",
            List.of("crm", "客户", "联系人", "商机", "跟进", "报价", "合同", "回款", "收款", "付款节点", "拜访", "电话沟通", "微信沟通", "邮件沟通"),
            "CRM 业务查询、跟进记录草稿和确认入库交给 crmBusiness。",
            "按需查询 CRM 客户、联系人、商机、跟进、报价、合同、回款信息"
        ),
        MES(
            "MES 工单",
            List.of("mes", "工单", "生产", "进度", "节点", "阶段", "报工"),
            "MES 工单、生产进度、工单节点交给 mesBusiness。",
            "按需查询 MES 工单、生产进度和阶段节点"
        ),
        ENGINEERING(
            "工程物料",
            List.of("工程", "物料", "清算", "缺料", "bom", "用料"),
            "工程物料、清算、缺料交给 engineeringBusiness。",
            "按需查询工程物料、清算和缺料信息"
        ),
        SRM(
            "SRM 采购",
            List.of("srm", "采购", "供应商", "采购需求", "采购订单", "下单"),
            "SRM 采购需求、采购订单交给 srmBusiness。",
            "按需查询 SRM 采购需求和采购订单"
        ),
        WMS(
            "WMS 库存",
            List.of("wms", "库存", "仓库", "收料", "发料", "入库", "出库", "物料编号"),
            "WMS 库存、收料、发料交给 wmsBusiness。",
            "按需查询 WMS 库存、收料和发料信息"
        );
//        XTP(
//            "XTP 制造闭环",
//            List.of("闭环", "制造闭环", "全流程", "做到哪一步", "交付进度", "从合同到", "合同后续"),
//            "合同后续制造闭环、跨 CRM/MES/SRM/WMS 的总览查询交给 xtpQuery。",
//            "按需查询 XTP 合同驱动制造闭环状态"
//        );

        private final String displayName;
        private final List<String> keywords;
        private final String supervisorHint;
        private final String planStep;

        BusinessDomain(String displayName, List<String> keywords, String supervisorHint, String planStep) {
            this.displayName = displayName;
            this.keywords = keywords;
            this.supervisorHint = supervisorHint;
            this.planStep = planStep;
        }

        boolean matches(String text) {
            for (String keyword : keywords) {
                if (text.contains(keyword)) {
                    return true;
                }
            }
            return false;
        }

        String supervisorHint() {
            return supervisorHint;
        }

        String planStep() {
            return planStep;
        }

        String displayName() {
            return displayName;
        }
    }

    private record AgentRuntimeProfile(EnumSet<BusinessDomain> businessDomains, boolean needsSql, boolean needsChart,
                                       boolean needsSkills, boolean needsExternalMcp) {

        static AgentRuntimeProfile from(String content) {
            String text = content == null ? "" : content.toLowerCase(Locale.ROOT);
            EnumSet<BusinessDomain> domains = EnumSet.noneOf(BusinessDomain.class);
            for (BusinessDomain domain : BusinessDomain.values()) {
                if (domain.matches(text)) {
                    domains.add(domain);
                }
            }
            boolean customerContractManufacturing = domains.contains(BusinessDomain.CRM)
                && containsAny(text, "合同")
                && containsAny(text, "工单", "物料", "物料清单", "生产进度", "交付进度", "进展", "阶段", "做到哪一步");
            if (customerContractManufacturing) {
                domains.clear();
                domains.add(BusinessDomain.CRM);
                domains.add(BusinessDomain.MES);
                domains.add(BusinessDomain.ENGINEERING);
                domains.add(BusinessDomain.SRM);
                domains.add(BusinessDomain.WMS);
            }
            boolean needsChart = containsAny(text, "图表", "echarts", "chart", "柱状图", "折线图", "饼图", "可视化");
            boolean needsSkills = containsAny(text, "word", "docx", "pdf", "excel", "xlsx", "文档", "表格", "文件生成", "报告");
            boolean needsExternalMcp = containsAny(text, "浏览器", "网页", "网址", "http", "https", "搜索", "联网", "playwright", "文件系统", "目录", "读取文件");
            boolean needsSql = containsAny(text, "sql", "数据库", "数据表", "表结构", "select ", "查询表");
            if (domains.isEmpty() && !needsSql && !needsChart && !needsSkills && !needsExternalMcp) {
                domains.add(BusinessDomain.CRM);
            }
            return new AgentRuntimeProfile(domains, needsSql, needsChart, needsSkills, needsExternalMcp);
        }

        boolean canRunDirectly() {
            return businessDomains.size() == 1 && !needsSql && !needsChart && !needsSkills && !needsExternalMcp;
        }

        boolean canRunSequentially() {
            return businessDomains.size() > 1 && !needsSql && !needsChart && !needsSkills && !needsExternalMcp;
        }

        BusinessDomain singleBusinessDomain() {
            return businessDomains.size() == 1 ? businessDomains.iterator().next() : null;
        }

        List<BusinessDomain> orderedBusinessDomains() {
            List<BusinessDomain> ordered = new ArrayList<>();
            for (BusinessDomain domain : BusinessDomain.values()) {
                if (businessDomains.contains(domain)) {
                    ordered.add(domain);
                }
            }
            return ordered;
        }

        List<String> planSteps() {
            List<String> steps = new ArrayList<>();
            steps.add(canRunDirectly() ? "识别为单业务域请求，跳过 Supervisor 直连子 Agent" : "理解用户问题并选择必要业务子 Agent");
            for (BusinessDomain domain : businessDomains) {
                steps.add(domain.planStep());
            }
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
