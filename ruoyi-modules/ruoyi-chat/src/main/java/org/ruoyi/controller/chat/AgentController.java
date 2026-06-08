package org.ruoyi.controller.chat;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.service.chat.impl.ChatServiceFacade;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Agent 对话入口。
 */
@Controller
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/agent")
public class AgentController {

    private final ChatServiceFacade chatService;

    /**
     * 内部统一入口 Agent 对话。
     */
    @PostMapping("/chat/{assistantCode}")
    @ResponseBody
    public SseEmitter agentChat(@PathVariable String assistantCode, @RequestBody @Valid ChatRequest chatRequest) {
        log.info("处理 Agent 对话, assistantCode={}, sessionId={}", assistantCode, chatRequest.getSessionId());
        return chatService.agentChat(assistantCode, chatRequest);
    }
}
