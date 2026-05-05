package com.clockshore.aooai.entity;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation{

    /**
     * 主键ID 数据库自增
     */
    private Long id;

    /**
     * 会话唯一标识（业务唯一键） 雪花id STR
     */
    private String conversationKey;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 会话ID（关联 chat_session.id） 暂时弃用
     */
    private Long sessionId;

    /**
     * 会话标题
     */
    private String title;

    /**
     * 状态：ACTIVE / DELETE
     */
    private String status;

    /**
     * 创建时间 数据库default
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间 数据库default
     */
    private LocalDateTime updatedAt;
}