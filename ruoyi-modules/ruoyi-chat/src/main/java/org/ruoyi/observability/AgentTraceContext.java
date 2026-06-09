package org.ruoyi.observability;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Carries the current agent run trace target across the async execution thread.
 */
public final class AgentTraceContext {

    private static final ThreadLocal<TraceTarget> CURRENT = new InheritableThreadLocal<>();

    private AgentTraceContext() {
    }

    public static void set(Long userId, String runId, SseEmitter emitter) {
        CURRENT.set(new TraceTarget(userId, runId, emitter));
    }

    public static TraceTarget get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }

    public record TraceTarget(Long userId, String runId, SseEmitter emitter) {
    }
}
