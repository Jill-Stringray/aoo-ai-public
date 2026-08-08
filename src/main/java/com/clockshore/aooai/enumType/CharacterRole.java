package com.clockshore.aooai.enumType;

import lombok.Getter;

@Getter
public enum CharacterRole {

    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant"),
    TOOL("tool");

    private final String value;

    CharacterRole(String value) {
        this.value = value;
    }
}
