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
    private Long messageId;
    private Long userId;
    private String Role;
    private String content;

}
