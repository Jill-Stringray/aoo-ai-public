package com.clockshore.aooai.controller;

import com.clockshore.aooai.chat.ChatRequest;
import com.clockshore.aooai.chat.ChatResponse;
import com.clockshore.aooai.chat.ConversationMessageDto;
import com.clockshore.aooai.entity.Conversation;
import com.clockshore.aooai.entity.ConversationMessage;
import com.clockshore.aooai.service.ChatWindowService;
import com.clockshore.aooai.service.DeepSeekChatService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("")
public class ChatController {

    private static final long DEFAULT_USER_ID = 1L;

    private final ChatWindowService chatWindowService;
    private final DeepSeekChatService deepSeekChatService;

    public ChatController(ChatWindowService chatWindowService, DeepSeekChatService deepSeekChatService) {
        this.chatWindowService = chatWindowService;
        this.deepSeekChatService = deepSeekChatService;
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        if (request == null || !StringUtils.hasText(request.resolveMessage())) {
            return ResponseEntity.badRequest()
                    .body(new ChatResponse("", "请输入 message，或提供含用户内容的 messages 数组"));
        }
        try {
            Conversation conversation = chatWindowService.saveUserMessage(DEFAULT_USER_ID, request);
            ChatRequest forModel = ChatRequest.builder()
                    .conversationId(conversation.getId())
                    .message(request.resolveMessage())
                    .userId(DEFAULT_USER_ID)
                    .build();
            String reply = deepSeekChatService.nonStreamChat(forModel);
            deepSeekChatService.enqueueAssistantMessage(forModel, reply);
            return ResponseEntity.ok(new ChatResponse(reply, "", conversation.getId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ChatResponse("", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ChatResponse("", "处理失败：" + e.getMessage()));
        }
    }

    @GetMapping("/conversation/{conversationId}/messages")
    public ResponseEntity<List<ConversationMessageDto>> conversationMessages(
            @PathVariable("conversationId") Long conversationId) {
        try {
            List<ConversationMessage> rows = chatWindowService.listMessages(DEFAULT_USER_ID, conversationId);
            return ResponseEntity.ok(rows.stream().map(ConversationMessageDto::from).toList());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(List.of());
        }
    }
}
