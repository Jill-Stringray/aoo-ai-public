package com.clockshore.aooai.mq;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 聊天消息持久化事件，字段与 conversation_message 表保持一致。
 */
@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageEvent implements Serializable {

    private Long conversationId;
    private String messageKey;
    private Long parentMessageId;
    private String role;
    private String content;
    private String modelName;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private Integer latencyMs;
    private String finishReason;
}
