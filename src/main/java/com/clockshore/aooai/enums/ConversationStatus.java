package com.clockshore.aooai.enums;

import lombok.Getter;

@Getter
public enum ConversationStatus {

    ACTIVE("active"),
    CLOSED("closed"),
    DELETED("deleted");

    private final String value;

    ConversationStatus(String value) {
        this.value = value;
    }

}