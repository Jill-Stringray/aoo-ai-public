package com.clockshore.aooai.controller;

import com.clockshore.aooai.common.ApiResponse;
import com.clockshore.aooai.dto.message.ChatMessage;
import com.clockshore.aooai.dto.message.ChatRequest;
import com.clockshore.aooai.dto.message.ChatRequestVO;
import com.clockshore.aooai.dto.message.ConversationVO;
import com.clockshore.aooai.security.SecurityUtils;
import com.clockshore.aooai.service.ChatService;
import com.clockshore.aooai.service.ConversationService;
import com.clockshore.aooai.service.SseService;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    @Resource
    private SseService sseService;

    @Resource
    private ChatService chatService;

    @Resource
    private ConversationService conversationService;

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestBody ChatRequest request) {
        return sseService.stream(() -> chatService.streamChat(request));
    }

    @GetMapping("/conversation/list")
    public ApiResponse<List<ConversationVO>> conversationList() {
        return ApiResponse.success(conversationService.fetchConversationList(SecurityUtils.getUserId()));
    }

    @GetMapping("/{conversationId}/messages")
    public ApiResponse<List<ChatMessage>> conversationMessage(
            @PathVariable("conversationId") Long conversationId,
            @RequestParam(value = "beforeId", required = false) Long beforeId,
            @RequestParam(value = "limit", defaultValue = "20") Integer limit) {
        return ApiResponse.success(
                conversationService.fetchConversationMessages(
                        SecurityUtils.getUserId(),
                        conversationId,
                        limit,
                        beforeId
                )
        );
    }

    @PutMapping("/conversation/{conversationId}/title")
    public ApiResponse<Void> renameConversation(
            @PathVariable("conversationId") Long conversationId,
            @RequestBody ChatRequestVO request) {
        conversationService.renameConversation(
                SecurityUtils.getUserId(),
                conversationId,
                request.getTitle()
        );
        return ApiResponse.success(null);
    }

    @DeleteMapping("/conversation/{conversationId}")
    public ApiResponse<Void> deleteConversation(@PathVariable("conversationId") Long conversationId) {
        conversationService.deleteConversation(SecurityUtils.getUserId(), conversationId);
        return ApiResponse.success(null);
    }
}
