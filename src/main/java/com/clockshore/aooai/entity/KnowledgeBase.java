package com.clockshore.aooai.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 公司内部知识库实体，对应 MySQL 表 knowledge_base。
 *
 * <p>Entity 层只表达数据库字段结构，不直接承载业务规则；
 * 复杂校验、状态流转和权限判断应放在 Service 层，便于后续维护和测试。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeBase {

    /**
     * 知识库ID。
     */
    private Long id;

    /**
     * 知识库编码，建议应用层保证唯一。
     */
    private String kbCode;

    /**
     * 知识库名称。
     */
    private String name;

    /**
     * 知识库描述。
     */
    private String description;

    /**
     * 负责人用户ID。
     */
    private Long ownerUserId;

    /**
     * 可见性：PRIVATE/DEPARTMENT/COMPANY。
     */
    private String visibility;

    /**
     * 状态：ACTIVE/ARCHIVED/DISABLED。
     */
    private String status;

    /**
     * 向量模型名称，便于模型升级追踪。
     */
    private String embeddingModel;

    /**
     * 检索参数 JSON，如 topK、相似度阈值、混合检索权重。
     */
    private String retrievalConfig;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
