package com.clockshore.aooai.controller;

import com.clockshore.aooai.auth.HttpSessionAuth;
import com.clockshore.aooai.chat.ChatRequest;
import com.clockshore.aooai.chat.ChatResponse123;
import com.clockshore.aooai.dto.ConversationCreateDto;
import com.clockshore.aooai.dto.ConversationMessageDto;
import com.clockshore.aooai.dto.ConversationPatchDto;
import com.clockshore.aooai.dto.ConversationSummaryDto;
import com.clockshore.aooai.entity.Conversation;
import com.clockshore.aooai.entity.ConversationMessage;
import com.clockshore.aooai.service.ChatWindowService;
import com.clockshore.aooai.service.DeepSeekChatService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("")
public class ChatController {

    private final ChatWindowService chatWindowService;
    private final DeepSeekChatService deepSeekChatService;

    public ChatController(ChatWindowService chatWindowService, DeepSeekChatService deepSeekChatService) {
        this.chatWindowService = chatWindowService;
        this.deepSeekChatService = deepSeekChatService;
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationSummaryDto>> listConversations(HttpServletRequest httpRequest) {
        Long uid = HttpSessionAuth.currentUserId(httpRequest);
        if (uid == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<ConversationSummaryDto> list = chatWindowService.listConversationsForUser(uid).stream()
                .map(ConversationSummaryDto::from)
                .toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping("/conversations")
    public ResponseEntity<?> createConversation(
            @RequestBody(required = false) ConversationCreateDto body,
            HttpServletRequest httpRequest) {
        Long uid = HttpSessionAuth.currentUserId(httpRequest);
        if (uid == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            String title = body != null ? body.getTitle() : null;
            Conversation c = chatWindowService.createConversation(uid, title);
            return ResponseEntity.status(HttpStatus.CREATED).body(ConversationSummaryDto.from(c));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<?> getConversation(
            @PathVariable("conversationId") Long conversationId,
            HttpServletRequest httpRequest) {
        Long uid = HttpSessionAuth.currentUserId(httpRequest);
        if (uid == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            Conversation c = chatWindowService.getConversation(uid, conversationId);
            return ResponseEntity.ok(ConversationSummaryDto.from(c));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }

    @PatchMapping("/conversations/{conversationId}")
    public ResponseEntity<?> patchConversation(
            @PathVariable("conversationId") Long conversationId,
            @RequestBody ConversationPatchDto body,
            HttpServletRequest httpRequest) {
        Long uid = HttpSessionAuth.currentUserId(httpRequest);
        if (uid == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (body == null || body.getTitle() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "请提供 title 字段（可为空字符串）"));
        }
        try {
            Conversation c = chatWindowService.updateConversationTitle(uid, conversationId, body.getTitle());
            return ResponseEntity.ok(ConversationSummaryDto.from(c));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/conversations/{conversationId}")
    public ResponseEntity<?> deleteConversation(
            @PathVariable("conversationId") Long conversationId,
            HttpServletRequest httpRequest) {
        Long uid = HttpSessionAuth.currentUserId(httpRequest);
        if (uid == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            chatWindowService.deleteConversation(uid, conversationId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse123> chat(@RequestBody ChatRequest request, HttpServletRequest httpRequest) {
        Long uid = HttpSessionAuth.currentUserId(httpRequest);
        if (uid == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ChatResponse123("", "请先登录"));
        }
        if (request == null || !StringUtils.hasText(request.resolveMessage())) {
            return ResponseEntity.badRequest()
                    .body(new ChatResponse123("", "请输入 message，或提供含用户内容的 messages 数组"));
        }
        try {
            Conversation conversation = chatWindowService.saveUserMessage(uid, request);
            ChatRequest forModel = ChatRequest.builder()
                    .conversationId(conversation.getId())
                    .message(request.resolveMessage())
                    .userId(uid)
                    .build();
            String reply = deepSeekChatService.nonStreamChat(forModel);
            deepSeekChatService.enqueueAssistantMessage(forModel, reply);
            return ResponseEntity.ok(new ChatResponse123(reply, "", conversation.getId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ChatResponse123("", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ChatResponse123("", "处理失败：" + e.getMessage()));
        }
    }

    @PostMapping(value = "/chat/stream", produces = "text/event-stream")
    public SseEmitter chatStream(@RequestBody ChatRequest request, HttpServletRequest httpRequest) {
        SseEmitter emitter = new SseEmitter(0L);
        Long uid = HttpSessionAuth.currentUserId(httpRequest);
        if (uid == null) {
            trySend(emitter, "error", "请先登录");
            emitter.complete();
            return emitter;
        }
        if (request == null || !StringUtils.hasText(request.resolveMessage())) {
            trySend(emitter, "error", "请输入 message，或提供含用户内容的 messages 数组");
            emitter.complete();
            return emitter;
        }

        try {
            Conversation conversation = chatWindowService.saveUserMessage(uid, request);
            ChatRequest forModel = ChatRequest.builder()
                    .conversationId(conversation.getId())
                    .message(request.resolveMessage())
                    .userId(uid)
                    .build();

            trySend(emitter, "meta", conversation.getId());

            Flux<ChatResponse> stream = deepSeekChatService.streamChat(forModel);
            stream.subscribe(
                    delta -> trySend(emitter, "delta", delta),
                    error -> {
                        trySend(emitter, "error", error.getMessage());
                        emitter.complete();
                    },
                    () -> {
                        trySend(emitter, "done", conversation.getId());
                        emitter.complete();
                    }
            );
        } catch (Exception e) {
            trySend(emitter, "error", e.getMessage());
            emitter.complete();
        }
        return emitter;
    }

    private void trySend(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    @GetMapping("/conversation/{conversationId}/messages")
    public ResponseEntity<List<ConversationMessageDto>> conversationMessages(
            @PathVariable("conversationId") Long conversationId,
            HttpServletRequest httpRequest) {
        Long uid = HttpSessionAuth.currentUserId(httpRequest);
        if (uid == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            List<ConversationMessage> rows = chatWindowService.listMessages(uid, conversationId);
            return ResponseEntity.ok(rows.stream().map(ConversationMessageDto::from).toList());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(List.of());
        }
    }
}
