package com.clockshore.aooai.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识库文档实体，对应 MySQL 表 knowledge_document。
 *
 * <p>Entity 层只表达数据库字段结构，不直接承载业务规则；
 * 复杂校验、状态流转和权限判断应放在 Service 层，便于后续维护和测试。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDocument {

    /**
     * 文档ID。
     */
    private Long id;

    /**
     * 知识库ID。
     */
    private Long knowledgeBaseId;

    /**
     * 文档业务唯一键，建议应用层保证唯一。
     */
    private String documentKey;

    /**
     * 文档标题。
     */
    private String title;

    /**
     * 原始文件名。
     */
    private String fileName;

    /**
     * 文件类型，如 PDF/DOCX/MD/HTML。
     */
    private String fileType;

    /**
     * MIME 类型。
     */
    private String mimeType;

    /**
     * 文件大小，单位字节。
     */
    private Long fileSize;

    /**
     * 对象存储地址或本地存储路径。
     */
    private String storageUrl;

    /**
     * 来源：UPLOAD/URL/API/MANUAL。
     */
    private String sourceType;

    /**
     * 状态：UPLOADED/PARSING/CHUNKING/INDEXING/READY/FAILED/DELETED。
     */
    private String status;

    /**
     * 文件摘要，用于去重和版本判断。
     */
    private String checksum;

    /**
     * 创建人。
     */
    private Long createdBy;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
