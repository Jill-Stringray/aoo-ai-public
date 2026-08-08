package com.clockshore.aooai.service;

import com.clockshore.aooai.dto.sse.SseMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
import reactor.core.Disposable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@Service
@Slf4j
public class SseService {

    /**
     * 永不超时
     */
    private static final long TIMEOUT = 0L;

    private final AtomicLong eventId = new AtomicLong();

    private final ConcurrentMap<SseEmitter, SseConnection> connections = new ConcurrentHashMap<>();

    public SseEmitter stream(Supplier<Flux<ChatResponse>> fluxSupplier) {

        SseEmitter emitter = createEmitter();

        try {
            //先createEmitter()再抛异常，避免SseEmitter还未创建就抛异常
            Flux<ChatResponse> flux = fluxSupplier.get();
            subscribe(emitter, flux);
        } catch (Exception e) {
            error(emitter, e.getMessage());
        }

        return emitter;
    }

    public SseEmitter stream(Flux<ChatResponse> flux) {

        SseEmitter emitter = createEmitter();

        subscribe(emitter, flux);

        return emitter;
    }

    private void subscribe(SseEmitter emitter, Flux<ChatResponse> flux) {

        SseConnection connection = connections.get(emitter);
        if (connection == null || connection.isClosed()) {
            return;
        }

        Disposable disposable = flux.subscribe(

                response -> {
                    if (connection.isClosed()) {
                        return;
                    }
                    send(emitter, "delta",
                            response.getResult().getOutput().getText());
                },

                error -> {
                    if (connection.isClosed()) {
                        log.debug("SSE flux error after connection closed", error);
                        return;
                    }
                    error(emitter, error.getMessage());
                },

                () -> {
                    if (connection.isClosed()) {
                        return;
                    }
                    complete(emitter);
                }

        );

        connection.setDisposable(disposable);
        if (connection.isClosed()) {
            disposable.dispose();
        }
    }

    /**
     * 创建连接
     */
    public SseEmitter createEmitter() {

        SseEmitter emitter = new SseEmitter(TIMEOUT);
        SseConnection connection = new SseConnection();
        connections.put(emitter, connection);

        emitter.onCompletion(() -> {
            close(emitter);
        });

        emitter.onTimeout(() -> close(emitter));

        emitter.onError(e -> close(emitter));

        return emitter;
    }

    /**
     * AI 回复 Token
     */
    public void sendToken(SseEmitter emitter, String token) {
        send(emitter, "delta", token);
    }

    /**
     * 检索到的引用
     */
    public void sendReference(SseEmitter emitter, Object reference) {
        send(emitter, "reference", reference);
    }

    /**
     * 状态
     */
    public void sendStatus(SseEmitter emitter, String status) {
        send(emitter, "status", status);
    }

    /**
     * 心跳
     */
    public void heartbeat(SseEmitter emitter) {
        send(emitter, "heartbeat", "ping");
    }

    /**
     * 完成
     */
    public void complete(SseEmitter emitter) {
        SseConnection connection = connections.get(emitter);
        if (connection == null || !send(emitter, "done", null)) {
            close(emitter);
            return;
        }
        if (connection.close()) {
            try {
                emitter.complete();
            } catch (IllegalStateException e) {
                log.debug("SSE emitter already completed", e);
            } finally {
                connections.remove(emitter);
            }
        }
    }

    /**
     * 错误
     */
    public void error(SseEmitter emitter, String message) {

        send(emitter, "error", message);

        close(emitter);
    }

    /**
     * 异常结束
     */
    public void completeWithError(SseEmitter emitter, Throwable e) {
        SseConnection connection = connections.get(emitter);
        if (connection == null || !connection.close()) {
            return;
        }
        try {
            emitter.completeWithError(e);
        } catch (IllegalStateException ex) {
            log.debug("SSE emitter already completed with error", ex);
        } finally {
            connections.remove(emitter);
        }
    }

    /**
     * 统一发送
     */
    private boolean send(
            SseEmitter emitter,
            String type,
            Object data
    ) {

        SseConnection connection = connections.get(emitter);
        if (connection == null || connection.isClosed()) {
            return false;
        }

        long id = eventId.incrementAndGet();

        SseMessage<Object> message =
                new SseMessage<>(id, data);

        try {
            emitter.send(
                    SseEmitter.event()
                            .id(String.valueOf(id))
                            .name(type)
                            .data(message)
            );
            return true;
        } catch (Exception e) {
            log.debug("SSE send failed, close emitter and cancel subscription", e);
            close(emitter);
            return false;
        }
    }

    private void close(SseEmitter emitter) {
        SseConnection connection = connections.get(emitter);
        if (connection == null || !connection.close()) {
            return;
        }
        connections.remove(emitter);
        try {
            emitter.complete();
        } catch (IllegalStateException e) {
            log.debug("SSE emitter already completed", e);
        }
    }

    private static class SseConnection {

        private final AtomicBoolean closed = new AtomicBoolean(false);

        private final AtomicReference<Disposable> disposableRef = new AtomicReference<>();

        private SseConnection() {
        }

        private void setDisposable(Disposable disposable) {
            disposableRef.set(disposable);
        }

        private boolean isClosed() {
            return closed.get();
        }

        private boolean close() {
            if (!closed.compareAndSet(false, true)) {
                return false;
            }
            Disposable disposable = disposableRef.getAndSet(null);
            if (disposable != null && !disposable.isDisposed()) {
                disposable.dispose();
            }
            return true;
        }
    }

}
