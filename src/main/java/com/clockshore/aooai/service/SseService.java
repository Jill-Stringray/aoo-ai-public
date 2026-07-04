package com.clockshore.aooai.service;

import com.clockshore.aooai.dto.sse.SseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

@Service
public class SseService {

    /**
     * 永不超时
     */
    private static final long TIMEOUT = 0L;

    private final AtomicLong eventId = new AtomicLong();


    public SseEmitter stream(Flux<ChatResponse> flux) {

        SseEmitter emitter = createEmitter();

        flux.subscribe(

                response -> {
                    try {
                        send(emitter, "delta",
                                response.getResult().getOutput().getText());
                    } catch (IOException e) {
                        completeWithError(emitter, e);
                    }
                },

                error -> error(emitter, error.getMessage()),

                () -> {
                    try {
                        complete(emitter);
                    } catch (IOException e) {
                        completeWithError(emitter, e);
                    }
                }

        );

        return emitter;
    }

    /**
     * 创建连接
     */
    public SseEmitter createEmitter() {

        SseEmitter emitter = new SseEmitter(TIMEOUT);

        emitter.onCompletion(() -> {
            // log.info("SSE completed");
        });

        emitter.onTimeout(emitter::complete);

        emitter.onError(emitter::completeWithError);

        return emitter;
    }

    /**
     * AI 回复 Token
     */
    public void sendToken(SseEmitter emitter, String token) throws IOException {
        send(emitter, "delta", token);
    }

    /**
     * 检索到的引用
     */
    public void sendReference(SseEmitter emitter, Object reference) throws IOException {
        send(emitter, "reference", reference);
    }

    /**
     * 状态
     */
    public void sendStatus(SseEmitter emitter, String status) throws IOException {
        send(emitter, "status", status);
    }

    /**
     * 心跳
     */
    public void heartbeat(SseEmitter emitter) throws IOException {
        send(emitter, "heartbeat", "ping");
    }

    /**
     * 完成
     */
    public void complete(SseEmitter emitter) throws IOException {
        send(emitter, "done", null);
        emitter.complete();
    }

    /**
     * 错误
     */
    public void error(SseEmitter emitter, String message) {

        try {
            send(emitter, "error", message);
        } catch (IOException ignored) {
        }

        emitter.complete();
    }

    /**
     * 异常结束
     */
    public void completeWithError(SseEmitter emitter, Throwable e) {
        emitter.completeWithError(e);
    }

    /**
     * 统一发送
     */
    private void send(
            SseEmitter emitter,
            String type,
            Object data
    ) throws IOException {

        long id = eventId.incrementAndGet();

        SseMessage<Object> message =
                new SseMessage<>(id, data);

        emitter.send(
                SseEmitter.event()
                        .id(String.valueOf(id))
                        .name(type)
                        .data(message)
        );
    }

}