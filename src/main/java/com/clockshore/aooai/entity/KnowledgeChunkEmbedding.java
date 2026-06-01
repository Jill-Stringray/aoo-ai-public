package com.clockshore.aooai.entity;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识切片向量实体，对应 PostgreSQL/pgvector 表 knowledge_chunk_embedding。
 *
 * <p>Entity 层只表达数据库字段结构，不直接承载业务规则；
 * 复杂校验、状态流转和权限判断应放在 Service 层，便于后续维护和测试。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeChunkEmbedding {

    /**
     * 向量记录ID。
     */
    private Long id;

    /**
     * 知识库ID。
     */
    private Long knowledgeBaseId;

    /**
     * 文档ID。
     */
    private Long documentId;

    /**
     * 切片ID。
     */
    private Long chunkId;

    /**
     * 切片业务唯一键。
     */
    private String chunkKey;

    /**
     * 向量模型名称。
     */
    private String embeddingModel;

    /**
     * 1536 维向量；落库时通常需要 pgvector 类型处理器或 Spring AI VectorStore 适配。
     */
    private List<Float> embedding;

    /**
     * 切片文本内容。
     */
    private String content;

    /**
     * 扩展元数据 JSONB。
     */
    private String metadata;

    /**
     * 创建时间。
     */
    private OffsetDateTime createdAt;

    /**
     * 更新时间。
     */
    private OffsetDateTime updatedAt;
}
