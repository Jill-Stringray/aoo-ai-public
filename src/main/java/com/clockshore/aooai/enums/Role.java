package com.clockshore.aooai.enums;

import lombok.Getter;

@Getter
public enum Role {

    USER("user"),
    ASSISTANT("assistant"),
    SYSTEM("system"),
    TOOL("tool");

    private final String value;

    Role(String value) {
        this.value = value;
    }
}
