package com.clockshore.aooai.dto.sse;

public record SseMessage<T>(
        Long id,
        T data
) {
}