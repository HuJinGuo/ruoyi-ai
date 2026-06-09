package org.ruoyi.observability;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agentic.observability.AfterAgentToolExecution;
import dev.langchain4j.agentic.observability.AgentInvocationError;
import dev.langchain4j.agentic.observability.AgentRequest;
import dev.langchain4j.agentic.observability.AgentResponse;
import dev.langchain4j.agentic.observability.BeforeAgentToolExecution;
import dev.langchain4j.agentic.planner.AgentInstance;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.service.tool.ToolExecution;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.sse.utils.SseMessageUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 自定义的 AgentListener 的监听器。
 * 监听 Agent 相关的所有可观测性事件，包括：
 * <ul>
 *   <li>Agent 调用前/后的生命周期事件</li>
 *   <li>Agent 执行错误事件</li>
 *   <li>AgenticScope 的创建/销毁事件</li>
 *   <li>工具执行前/后的生命周期事件</li>
 * </ul>
 *
 * @author evo
 */
@Slf4j
public class MyAgentListener implements dev.langchain4j.agentic.observability.AgentListener {

    /** 最终捕获到的思考结果（主 Agent 完成后写入，供外部获取） */
    private final AtomicReference<String> sharedOutputRef = new AtomicReference<>();
    private final Long userId;
    private final String runId;
    private final SseEmitter emitter;

    public MyAgentListener() {
        this.userId = null;
        this.runId = null;
        this.emitter = null;
    }

    public MyAgentListener(Long userId) {
        this.userId = userId;
        this.runId = null;
        this.emitter = null;
    }

    public MyAgentListener(Long userId, String runId) {
        this.userId = userId;
        this.runId = runId;
        this.emitter = null;
    }

    public MyAgentListener(Long userId, String runId, SseEmitter emitter) {
        this.userId = userId;
        this.runId = runId;
        this.emitter = emitter;
    }

    public String getCapturedResult() {
        return sharedOutputRef.get();
    }

    // ==================== Agent 调用生命周期 ====================

    @Override
    public void beforeAgentInvocation(AgentRequest agentRequest) {
        AgentInstance agent = agentRequest.agent();
        AgenticScope scope = agentRequest.agenticScope();
        Map<String, Object> inputs = agentRequest.inputs();

        log.info("【Agent调用前】Agent名称: {}", agent.name());
        log.info("【Agent调用前】Agent ID: {}", agent.agentId());
        log.info("【Agent调用前】Agent类型: {}", agent.type().getName());
        log.info("【Agent调用前】Agent描述: {}", agent.description());
        log.info("【Agent调用前】Planner类型: {}", agent.plannerType());
        log.info("【Agent调用前】输出类型: {}", agent.outputType());
        log.info("【Agent调用前】输出Key: {}", agent.outputKey());
        log.info("【Agent调用前】是否为异步: {}", agent.async());
        log.info("【Agent调用前】是否为叶子节点: {}", agent.leaf());
        log.info("【Agent调用前】Agent参数列表:");
        for (var arg : agent.arguments()) {
            log.info("  - 参数名: {}, 类型: {}, 默认值: {}",
                    arg.name(), arg.rawType().getName(), arg.defaultValue());
        }
        log.info("【Agent调用前】Agent输入参数: {}", inputs);
        log.info("【Agent调用前】AgenticScope memoryId: {}", scope.memoryId());
        log.info("【Agent调用前】AgenticScope当前状态: {}", scope.state());
        log.info("【Agent调用前】Agent调用历史记录数: {}", scope.agentInvocations().size());
        pushTrace("agent_step_start", agent.name(), agent.agentId(), "AGENT", "running", inputs, null,
            agentStartMessage(agent));

        // 打印嵌套的子Agent信息
        if (!agent.subagents().isEmpty()) {
            log.info("【Agent调用前】子Agent列表:");
            for (AgentInstance sub : agent.subagents()) {
                log.info("  - 子Agent: {} ({})", sub.name(), sub.type().getName());
            }
        }

        // 打印父Agent信息
        if (agent.parent() != null) {
            log.info("【Agent调用前】父Agent: {}", agent.parent().name());
        }
    }

    @Override
    public void afterAgentInvocation(AgentResponse agentResponse) {
        AgentInstance agent = agentResponse.agent();
        Map<String, Object> inputs = agentResponse.inputs();
        Object output = agentResponse.output();
        String outputStr = output != null ? output.toString() : "";

        log.info("【Agent调用后】Agent名称: {}", agent.name());
        log.info("【Agent调用后】Agent ID: {}", agent.agentId());
        log.info("【Agent调用后】Agent输入参数: {}", inputs);
        log.info("【Agent调用后】Agent输出结果: {}", output);
        log.info("【Agent调用后】是否为叶子节点: {}", agent.leaf());

        // 捕获主 Agent 的最终输出，供外部获取
        if ("invoke".equals(agent.agentId()) && !outputStr.isEmpty()) {
            sharedOutputRef.set(outputStr);
            log.info("【Agent调用后】已捕获主Agent输出: {}", outputStr);
        }
        String summary = summarizeAgentOutput(agent.name(), outputStr);
        pushTrace("agent_step_done", agent.name(), agent.agentId(), "AGENT", "success", inputs, summary,
            agentDoneMessage(agent, summary));
    }

    @Override
    public void onAgentInvocationError(AgentInvocationError error) {
        AgentInstance agent = error.agent();
        Map<String, Object> inputs = error.inputs();
        Throwable throwable = error.error();

        log.error("【Agent执行错误】Agent名称: {}", agent.name());
        log.error("【Agent执行错误】Agent ID: {}", agent.agentId());
        log.error("【Agent执行错误】Agent类型: {}", agent.type().getName());
        log.error("【Agent执行错误】Agent输入参数: {}", inputs);
        log.error("【Agent执行错误】错误类型: {}", throwable.getClass().getName());
        log.error("【Agent执行错误】错误信息: {}", throwable.getMessage(), throwable);
        pushTrace("agent_step_done", agent.name(), agent.agentId(), "AGENT", "error", inputs, throwable.getMessage(),
            agentFriendlyName(agent.name()) + "执行失败：" + throwable.getMessage());
    }

    // ==================== AgenticScope 生命周期 ====================

    @Override
    public void afterAgenticScopeCreated(AgenticScope agenticScope) {
        log.info("【AgenticScope已创建】memoryId: {}", agenticScope.memoryId());
        log.info("【AgenticScope已创建】初始状态: {}", agenticScope.state());
    }

    @Override
    public void beforeAgenticScopeDestroyed(AgenticScope agenticScope) {
        log.info("【AgenticScope即将销毁】memoryId: {}", agenticScope.memoryId());
        log.info("【AgenticScope即将销毁】最终状态: {}", agenticScope.state());
        log.info("【AgenticScope即将销毁】总调用次数: {}", agenticScope.agentInvocations().size());
    }

    // ==================== 工具执行生命周期 ====================

    @Override
    public void beforeAgentToolExecution(BeforeAgentToolExecution beforeAgentToolExecution) {
        ToolExecutionRequest request = beforeAgentToolExecution.toolExecution().request();
        AgentInstance agent = beforeAgentToolExecution.agentInstance();

        log.info("【Agent工具调用前】Agent: {}, 工具: {}, 参数: {}", agent.name(), request.name(), request.arguments());
        pushToolTrace("agent_tool_start", agent, request, "running", null, null, "工具「" + request.name() + "」开始执行");
    }

    @Override
    public void afterAgentToolExecution(AfterAgentToolExecution afterAgentToolExecution) {
        ToolExecution toolExecution = afterAgentToolExecution.toolExecution();
        ToolExecutionRequest request = toolExecution.request();
        AgentInstance agent = afterAgentToolExecution.agentInstance();
        String status = toolExecution.hasFailed() ? "error" : "success";
        String result = safeToolResult(toolExecution);
        Duration duration = toolExecution.duration();

        log.info("【Agent工具调用后】Agent: {}, 工具: {}, 状态: {}, 结果: {}", agent.name(), request.name(), status, result);
        if (!AgentTraceContext.markToolDone(toolTraceId(request))) {
            return;
        }
        String message = toolExecution.hasFailed()
            ? "工具「" + request.name() + "」执行失败"
            : "工具「" + request.name() + "」已执行完成";
        pushToolTrace("agent_tool_done", agent, request, status, result, duration, message);
    }

    private void pushTrace(String event, String name, String stepId, String type, String status, Object input,
                           Object result, String message) {
        if (userId == null) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        if (runId != null) {
            payload.put("runId", runId);
        }
        payload.put("stepId", stepId);
        payload.put("name", name);
        payload.put("type", type);
        payload.put("status", status);
        payload.put("timestamp", System.currentTimeMillis());
        if (input != null) {
            payload.put("input", input);
        }
        if (result != null) {
            payload.put("result", result);
        }
        if (message != null && !message.isBlank()) {
            payload.put("message", message);
        }
        if (emitter != null) {
            SseMessageUtils.sendAgentEvent(emitter, event, payload);
        } else {
            SseMessageUtils.sendAgentEvent(userId, event, payload);
        }
    }

    private void pushToolTrace(String event, AgentInstance agent, ToolExecutionRequest request, String status,
                               String result, Duration duration, String message) {
        if (userId == null) {
            return;
        }
        AgentTraceContext.TraceTarget target = AgentTraceContext.get();
        Map<String, Object> payload = new LinkedHashMap<>();
        if (runId != null) {
            payload.put("runId", runId);
        } else if (target != null && target.runId() != null) {
            payload.put("runId", target.runId());
        }
        payload.put("stepId", "tool:" + toolTraceId(request));
        if (target != null && target.stepId() != null) {
            payload.put("parentStepId", target.stepId());
        }
        if (target != null && target.stepName() != null) {
            payload.put("parentStepName", target.stepName());
        }
        payload.put("id", request.id());
        payload.put("name", request.name());
        payload.put("type", "TOOL");
        payload.put("status", status);
        payload.put("timestamp", System.currentTimeMillis());
        payload.put("input", request.arguments());
        if (result != null) {
            payload.put("result", truncate(result, 1200));
        }
        payload.put("message", message);
        payload.put("agentName", agent.name());
        payload.put("agentId", agent.agentId());
        if (duration != null) {
            payload.put("durationMs", duration.toMillis());
        }
        if (emitter != null) {
            SseMessageUtils.sendAgentEvent(emitter, event, payload);
        } else {
            SseMessageUtils.sendAgentEvent(userId, event, payload);
        }
    }

    private String agentStartMessage(AgentInstance agent) {
        String description = agent.description();
        if (description == null || description.isBlank()) {
            return agentFriendlyName(agent.name()) + "开始处理";
        }
        return agentFriendlyName(agent.name()) + "开始处理：" + description;
    }

    private String agentDoneMessage(AgentInstance agent, String summary) {
        return agentFriendlyName(agent.name()) + "已完成。" + summary;
    }

    private String summarizeAgentOutput(String agentName, String output) {
        String friendlyName = agentFriendlyName(agentName);
        if (output == null || output.isBlank()) {
            return friendlyName + "未返回可展示的业务结果";
        }

        String plain = compactPlainText(output);
        boolean hasGap = containsAny(plain, "暂未查询到", "未查询到", "缺少", "无法唯一", "需要补充", "异常", "失败", "逾期", "风险");
        List<String> highlights = extractHighlights(output);
        StringBuilder summary = new StringBuilder();
        summary.append(friendlyName).append("已完成处理，");
        summary.append(hasGap ? "存在需要补充或关注的信息" : "已返回可用业务线索");
        summary.append("，结果约").append(plain.length()).append("字");
        if (!highlights.isEmpty()) {
            summary.append("，关键线索：").append(String.join("、", highlights));
        }
        return summary.toString();
    }

    private List<String> extractHighlights(String output) {
        List<String> highlights = new ArrayList<>();
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\*\\*([^*\\n]{2,50})\\*\\*").matcher(output);
        while (matcher.find() && highlights.size() < 4) {
            String value = matcher.group(1).trim();
            if (!value.isBlank() && !highlights.contains(value)) {
                highlights.add(value);
            }
        }
        if (highlights.isEmpty()) {
            String sentence = firstSentence(compactPlainText(output));
            if (!sentence.isBlank()) {
                highlights.add(truncate(sentence, 80));
            }
        }
        return highlights;
    }

    private String firstSentence(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        int end = text.length();
        for (String mark : List.of("。", "；", ";", "\n")) {
            int index = text.indexOf(mark);
            if (index > 0) {
                end = Math.min(end, index);
            }
        }
        return text.substring(0, Math.min(end, text.length())).trim();
    }

    private String compactPlainText(String value) {
        if (value == null) {
            return "";
        }
        return value
            .replaceAll("<[^>]+>", "")
            .replace("|", " ")
            .replace("*", "")
            .replace("#", "")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null) {
            return false;
        }
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String agentFriendlyName(String agentName) {
        if ("crmBusiness".equals(agentName)) {
            return "CRM 业务 Agent";
        }
        if ("mesBusiness".equals(agentName)) {
            return "MES 工单 Agent";
        }
        if ("engineeringBusiness".equals(agentName)) {
            return "工程物料 Agent";
        }
        if ("srmBusiness".equals(agentName)) {
            return "SRM 采购 Agent";
        }
        if ("wmsBusiness".equals(agentName)) {
            return "WMS 库存 Agent";
        }
        if ("sqlQuery".equals(agentName)) {
            return "SQL 查询 Agent";
        }
        if ("chartGeneration".equals(agentName) || "echarts".equals(agentName)) {
            return "图表生成 Agent";
        }
        if ("skills".equals(agentName)) {
            return "文档技能 Agent";
        }
        if ("webSearch".equals(agentName)) {
            return "网页/文件工具 Agent";
        }
        return "Agent「" + agentName + "」";
    }

    private String safeToolResult(ToolExecution toolExecution) {
        try {
            return toolExecution.result();
        } catch (Exception e) {
            try {
                return String.valueOf(toolExecution.resultObject());
            } catch (Exception ignored) {
                return "";
            }
        }
    }

    private String toolTraceId(ToolExecutionRequest request) {
        if (request.id() != null) {
            return request.id();
        }
        return request.name() + ":" + Integer.toHexString(String.valueOf(request.arguments()).hashCode());
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }
}
