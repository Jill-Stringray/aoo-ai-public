package com.clockshore.aooai.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 消息召回来源实体，对应 MySQL 表 message_retrieval_source。
 *
 * <p>Entity 层只表达数据库字段结构，不直接承载业务规则；
 * 复杂校验、状态流转和权限判断应放在 Service 层，便于后续维护和测试。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageRetrievalSource {

    /**
     * 召回来源ID。
     */
    private Long id;

    /**
     * AI 回答消息ID。
     */
    private String messageKey;

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
     * 页码。
     */
    private Integer pageNumber;

    /**
     * 召回排序。
     */
    private Integer rankNo;

    /**
     * 综合相似度得分。
     */
    private Double score;

    /**
     * 重排得分。
     */
    private Double rerankScore;

    /**
     * 回答引用片段快照。
     */
    private String quoteText;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
}
