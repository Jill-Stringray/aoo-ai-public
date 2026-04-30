package com.clockshore.aooai.Entity;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMessage {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 会话ID（关联 conversation.id）
     */
    private Long conversationId;

    /**
     * 消息内容（可能很长，所以是 text/memo）
     */
    private String content;

    /**
     * token数量（用于大模型计费/统计）
     */
    private Integer tokenCount;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 消息角色：system / user / assistant / tool
     */
    private String role;
}
