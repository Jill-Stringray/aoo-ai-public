package com.clockshore.aooai.mq;

import java.io.Serializable;

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

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
