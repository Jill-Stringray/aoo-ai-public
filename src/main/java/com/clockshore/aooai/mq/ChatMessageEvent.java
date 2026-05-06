package com.clockshore.aooai.mq;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
@Builder
public class ChatMessageEvent implements Serializable {

    private Long conversationId;
    private String messageKey;
    private Long userId;
    private String role;
    private String content;

}
