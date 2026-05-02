package com.clockshore.aooai.Entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SseMessage<T> {

    private String type;
    private Long eventId;
    private T data;

    public SseMessage() {
    }

    public SseMessage(String type, Long eventId, T data) {
        this.type = type;
        this.eventId = eventId;
        this.data = data;
    }
}