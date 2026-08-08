package com.clockshore.aooai.enumType;

import lombok.Getter;

@Getter
public enum LocalMqMessageStatus {

    NEW("NEW"),
    SENDING("SENDING"),
    SENT("SENT"),
    FAILED("FAILED");

    private final String value;

    LocalMqMessageStatus(String value) {
        this.value = value;
    }
}