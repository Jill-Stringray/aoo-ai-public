package com.clockshore.aooai.mq;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
public class ChatMessageEvent implements Serializable {

    private Long conversationId;
    private Long messageId;
    private Long userId;
    private String content;

    public ChatMessageEvent() {
    }

    public ChatMessageEvent(Long conversationId, Long messageId, Long userId, String content) {
        this.conversationId = conversationId;
        this.messageId = messageId;
        this.userId = userId;
        this.content = content;
    }

}
