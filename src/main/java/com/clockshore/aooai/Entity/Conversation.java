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
public class Conversation{

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 会话唯一标识（业务唯一键）
     */
    private String conversationKey;

    /**
     * 用户ID（关联 app_user.id）
     */
    private Long userId;

    /**
     * 会话ID（关联 chat_session.id）
     */
    private Long sessionId;

    /**
     * 会话标题
     */
    private String title;

    /**
     * 状态：ACTIVE / CLOSED 等
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}