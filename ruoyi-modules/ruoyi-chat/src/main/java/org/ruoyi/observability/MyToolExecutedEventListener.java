package org.ruoyi.observability;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.observability.api.event.ToolExecutedEvent;
import dev.langchain4j.observability.api.listener.ToolExecutedEventListener;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.sse.utils.SseMessageUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 自定义的 ToolExecutedEvent 的监听器。
 * 它表示在工具执行完成后发生的事件。
 * 在单个 AI 服务调用期间，可能会被调用多次。
 *
 * @author evo
 */
@Slf4j
public class MyToolExecutedEventListener implements ToolExecutedEventListener {

    @Override
    public void onEvent(ToolExecutedEvent event) {
        InvocationContext invocationContext = event.invocationContext();
        UUID invocationId = invocationContext.invocationId();
        String aiServiceInterfaceName = invocationContext.interfaceName();
        String aiServiceMethodName = invocationContext.methodName();
        ToolExecutionRequest request = event.request();
        String resultText = safeResultText(event);

        log.info("【工具已执行】调用唯一标识符: {}", invocationId);
        log.info("【工具已执行】AI服务接口名: {}", aiServiceInterfaceName);
        log.info("【工具已执行】调用的方法名: {}", aiServiceMethodName);
        log.info("【工具已执行】工具执行请求 ID: {}", request.id());
        log.info("【工具已执行】工具名称: {}", request.name());
        log.info("【工具已执行】工具参数: {}", request.arguments());
        log.info("【工具已执行】工具执行结果: {}", resultText);
        pushToolTrace(request, resultText, aiServiceInterfaceName, aiServiceMethodName);
    }

    private void pushToolTrace(ToolExecutionRequest request, String resultText, String aiServiceInterfaceName,
                               String aiServiceMethodName) {
        AgentTraceContext.TraceTarget target = AgentTraceContext.get();
        if (target == null || target.userId() == null) {
            return;
        }
        String toolTraceId = toolTraceId(request);
        if (!AgentTraceContext.markToolDone(toolTraceId)) {
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        if (target.runId() != null) {
            payload.put("runId", target.runId());
        }
        payload.put("stepId", "tool:" + toolTraceId);
        if (target.stepId() != null) {
            payload.put("parentStepId", target.stepId());
        }
        if (target.stepName() != null) {
            payload.put("parentStepName", target.stepName());
        }
        payload.put("id", request.id());
        payload.put("name", request.name());
        payload.put("type", "TOOL");
        payload.put("status", "success");
        payload.put("timestamp", System.currentTimeMillis());
        payload.put("input", request.arguments());
        payload.put("result", truncate(resultText, 1200));
        payload.put("message", "工具「" + request.name() + "」已执行完成");
        payload.put("aiService", aiServiceInterfaceName);
        payload.put("aiServiceMethod", aiServiceMethodName);

        if (target.emitter() != null) {
            SseMessageUtils.sendAgentEvent(target.emitter(), "agent_tool_done", payload);
        } else {
            SseMessageUtils.sendAgentEvent(target.userId(), "agent_tool_done", payload);
        }
    }

    private String safeResultText(ToolExecutedEvent event) {
        try {
            return event.resultText();
        } catch (Exception e) {
            try {
                return String.valueOf(event.resultContents());
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
