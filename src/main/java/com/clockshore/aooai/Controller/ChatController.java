package com.clockshore.aooai.Controller;

import com.clockshore.aooai.chat.ChatRequest;
import com.clockshore.aooai.chat.ChatResponse;
import com.clockshore.aooai.chat.ConversationMessageDto;
import com.clockshore.aooai.Entity.Conversation;
import com.clockshore.aooai.Entity.ConversationMessage;
import com.clockshore.aooai.Service.ChatWindowService;
import com.clockshore.aooai.Service.DeepSeekChatService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("")
public class ChatController {

    private final DeepSeekChatService deepSeekChatService;
    private final ChatWindowService chatWindowService;

    public ChatController(DeepSeekChatService deepSeekChatService, ChatWindowService chatWindowService) {
        this.deepSeekChatService = deepSeekChatService;
        this.chatWindowService = chatWindowService;
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request,
                                             @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (request == null || !StringUtils.hasText(request.resolveMessage())) {
            return ResponseEntity.badRequest().body(new ChatResponse(null, "请输入消息，或提供符合 DeepSeek 格式的 messages 数组"));
        }

        try {
            Long effectiveUserId = userId == null ? 1L : userId;
            Conversation conversation = chatWindowService.sendMessage(effectiveUserId, request.getConversationId(), request.resolveMessage());
            String reply = deepSeekChatService.chat(request);
            chatWindowService.saveAssistantReply(effectiveUserId, conversation.getId(), reply);
            return ResponseEntity.ok(new ChatResponse(reply, "请求正常完成", conversation.getId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ChatResponse(null, "发生错误：" + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ChatResponse(null, "调用 DeepSeek 失败：" + e.getMessage()));
        }
    }

    @GetMapping("/conversation/{conversationId}/messages")
    public ResponseEntity<List<ConversationMessageDto>> conversationMessages(
            @PathVariable("conversationId") Long conversationId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        Long effectiveUserId = userId == null ? 1L : userId;
        List<ConversationMessage> messages = chatWindowService.listMessages(effectiveUserId, conversationId);
        return ResponseEntity.ok(
                messages.stream().map(ConversationMessageDto::from).toList()
        );
    }
}
