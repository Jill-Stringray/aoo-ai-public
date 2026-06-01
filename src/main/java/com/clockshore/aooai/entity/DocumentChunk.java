package com.clockshore.aooai.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档切片实体，对应 MySQL 表 document_chunk。
 *
 * <p>Entity 层只表达数据库字段结构，不直接承载业务规则；
 * 复杂校验、状态流转和权限判断应放在 Service 层，便于后续维护和测试。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChunk {

    /**
     * 切片ID。
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
     * 切片业务唯一键，与向量库 metadata 对齐。
     */
    private String chunkKey;

    /**
     * 文档内切片序号。
     */
    private Integer chunkIndex;

    /**
     * 切片文本内容。
     */
    private String content;

    /**
     * 切片内容摘要。
     */
    private String contentHash;

    /**
     * token 数。
     */
    private Integer tokenCount;

    /**
     * 页码。
     */
    private Integer pageNo;

    /**
     * 章节标题。
     */
    private String sectionTitle;

    /**
     * 页码、标题层级、表格信息等扩展元数据 JSON。
     */
    private String metadata;

    /**
     * pgvector 记录ID或外部向量库ID。
     */
    private String vectorRef;

    /**
     * 状态：NEW/EMBEDDED/FAILED/DELETED。
     */
    private String status;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
