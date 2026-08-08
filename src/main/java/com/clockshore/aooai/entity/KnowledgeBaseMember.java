package com.clockshore.aooai.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识库成员授权实体，对应 MySQL 表 knowledge_base_member。
 *
 * <p>Entity 层只表达数据库字段结构，不直接承载业务规则；
 * 复杂校验、状态流转和权限判断应放在 Service 层，便于后续维护和测试。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeBaseMember {

    /**
     * 成员关系ID。
     */
    private Long id;

    /**
     * 知识库ID。
     */
    private Long knowledgeBaseId;

    /**
     * 用户ID。
     */
    private Long userId;

    /**
     * 成员角色：OWNER/EDITOR/READER。
     */
    private String memberRole;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
}
