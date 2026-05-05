package com.clockshore.aooai.controller;

import com.clockshore.aooai.service.ChatWindowService;
import com.clockshore.aooai.service.DeepSeekChatService;
import com.clockshore.aooai.chat.ChatRequest;
import com.clockshore.aooai.config.ChatStreamExecutorConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@RestController
@RequestMapping("")
public class ChatStreamController {

    private static final long SSE_TIMEOUT_MS = 900_000L;

    private final ChatWindowService chatWindowService;
    private final DeepSeekChatService deepSeekChatService;
    private final Executor chatStreamExecutor;

    public ChatStreamController(
            ChatWindowService chatWindowService,
            DeepSeekChatService deepSeekChatService,
            @Qualifier(ChatStreamExecutorConfig.CHAT_STREAM_EXECUTOR) Executor chatStreamExecutor) {
        this.chatWindowService = chatWindowService;
        this.deepSeekChatService = deepSeekChatService;
        this.chatStreamExecutor = chatStreamExecutor;
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody ChatRequest request,
                                 @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        if (request == null || !StringUtils.hasText(request.resolveMessage())) {
            runAsyncEmitValidationError(emitter, "请输入消息，或提供符合 DeepSeek 格式的 messages 数组");
            return emitter;
        }

        Long effectiveUserId = userId == null ? 1L : userId;

        CompletableFuture.runAsync(() -> {
            try {
                ChatWindowService.UserMessagePersisted saved = chatWindowService.saveUserMessageWithoutMq(effectiveUserId, request);
                Long conversationId = saved.conversation().getId();
                send(emitter, "meta", Map.of("conversationId", conversationId));

                ChatRequest withHistory = chatWindowService.buildChatRequestWithHistory(
                        effectiveUserId, conversationId, saved.message().getId());

                String full = deepSeekChatService.streamChat(withHistory, piece -> {
                    try {
                        send(emitter, "delta", Map.of("c", piece));
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                });

                if (!StringUtils.hasText(full)) {
                    full = "(模型未返回内容)";
                }
                chatWindowService.saveAssistantReply(effectiveUserId, conversationId, full);
                send(emitter, "done", Map.of("ok", true));
                emitter.complete();
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : "处理失败";
                try {
                    send(emitter, "error", Map.of("message", msg));
                } catch (IOException ignored) {
                    // fall through
                }
                emitter.completeWithError(e);
            }
        }, chatStreamExecutor);

        return emitter;
    }

    private void runAsyncEmitValidationError(SseEmitter emitter, String message) {
        CompletableFuture.runAsync(() -> {
            try {
                send(emitter, "error", Map.of("message", message));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }, chatStreamExecutor);
    }

    private static void send(SseEmitter emitter, String eventName, Object data) throws IOException {
        emitter.send(SseEmitter.event().name(eventName).data(data));
    }
}
