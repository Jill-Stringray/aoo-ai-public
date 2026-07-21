package com.clockshore.aooai.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 问答会话实体，对应 MySQL 表 conversation。
 *
 * <p>Entity 层只表达数据库字段结构，不直接承载业务规则；
 * 复杂校验、状态流转和权限判断应放在 Service 层，便于后续维护和测试。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {

    /**
     * 会话ID。
     */
    private Long id;

    /**
     * 会话业务唯一键，建议应用层保证唯一。
     */
    private String conversationKey;

    /**
     * 用户ID。
     */
    private Long userId;

    /**
     * 本会话默认知识库。
     */
    private Long knowledgeBaseId;

    /**
     * 会话标题。
     */
    private String title;

    /**
     * 状态：ACTIVE/ARCHIVED/DELETED。
     */
    private String status;

    /**
     * 是否收藏。
     */
    private Boolean isFavorite;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
