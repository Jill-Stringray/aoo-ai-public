package com.clockshore.aooai.service;


import com.clockshore.aooai.entity.SseMessage;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class SseService {

    private static final long SSE_TIMEOUT_MILLIS = 0L;

    //线程安全，防止并发出错
    private final Set<SseEmitter> emitters = ConcurrentHashMap.newKeySet();
    private final AtomicLong eventIdGenerator = new AtomicLong(0);

    public SseEmitter createEmitter() {
        final SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((error) -> emitters.remove(emitter));
        return emitter;
    }

    public void sendHeartbeatToAll() {
        broadcast("heartbeat", "connected");
    }

    public void sendHeartbeat(SseEmitter emitter) throws IOException {
        sendToEmitter(emitter, "heartbeat", "connected");
    }

    public void broadcast(String type, Object data) {
        long eventId = eventIdGenerator.incrementAndGet();
        for (SseEmitter emitter : emitters) {
            try {
                sendToEmitter(emitter, type, data, eventId);
            } catch (IOException e) {
                emitters.remove(emitter);
                emitter.completeWithError(e);
            }
        }
    }

    private void sendToEmitter(SseEmitter emitter, String type, Object data) throws IOException {
        long eventId = eventIdGenerator.incrementAndGet();
        sendToEmitter(emitter, type, data, eventId);
    }

    private void sendToEmitter(SseEmitter emitter, String type, Object data, long eventId) throws IOException {
        SseMessage<Object> message = new SseMessage<>(type, eventId, data);
        emitter.send(
                SseEmitter.event()
                        .id(String.valueOf(eventId))
                        .name("message")
                        .data(message)
        );
    }

    public int onlineCount() {
        return emitters.size();
    }
}
