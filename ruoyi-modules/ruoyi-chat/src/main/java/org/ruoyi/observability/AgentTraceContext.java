package org.ruoyi.observability;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Carries the current agent run trace target across the async execution thread.
 */
public final class AgentTraceContext {

    private static final ThreadLocal<TraceTarget> CURRENT = new InheritableThreadLocal<>();

    private AgentTraceContext() {
    }

    public static void set(Long userId, String runId, SseEmitter emitter) {
        CURRENT.set(new TraceTarget(userId, runId, emitter, null, null, ConcurrentHashMap.newKeySet()));
    }

    public static TraceTarget get() {
        return CURRENT.get();
    }

    public static void enterStep(String stepId, String stepName) {
        TraceTarget target = CURRENT.get();
        if (target == null) {
            return;
        }
        CURRENT.set(target.withStep(stepId, stepName));
    }

    public static void clearStep(String stepId) {
        TraceTarget target = CURRENT.get();
        if (target == null || stepId == null || !stepId.equals(target.stepId())) {
            return;
        }
        CURRENT.set(target.withStep(null, null));
    }

    public static boolean markToolDone(String toolTraceId) {
        TraceTarget target = CURRENT.get();
        if (target == null || toolTraceId == null) {
            return true;
        }
        return target.doneToolTraceIds().add(toolTraceId);
    }

    public static void clear() {
        CURRENT.remove();
    }

    public record TraceTarget(Long userId, String runId, SseEmitter emitter, String stepId, String stepName,
                              Set<String> doneToolTraceIds) {

        public TraceTarget {
            if (doneToolTraceIds == null) {
                doneToolTraceIds = ConcurrentHashMap.newKeySet();
            }
        }

        private TraceTarget withStep(String nextStepId, String nextStepName) {
            return new TraceTarget(userId, runId, emitter, nextStepId, nextStepName, doneToolTraceIds);
        }
    }
}
