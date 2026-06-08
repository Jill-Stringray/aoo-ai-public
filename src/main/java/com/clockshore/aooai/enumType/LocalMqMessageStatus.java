package com.clockshore.aooai.enumType;

import lombok.Getter;

@Getter
public enum LocalMqMessageStatus {

    NEW("new"),
    SENDING("sending"),
    SENT("sent"),
    FAILED("failed");

    private final String value;

    LocalMqMessageStatus(String value) {
        this.value = value;
    }
}