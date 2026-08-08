package com.clockshore.aooai.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 索引任务实体，对应 MySQL 表 index_task。
 *
 * <p>Entity 层只表达数据库字段结构，不直接承载业务规则；
 * 复杂校验、状态流转和权限判断应放在 Service 层，便于后续维护和测试。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndexTask {

    /**
     * 任务ID。
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
     * 任务类型：PARSE/CHUNK/EMBED/REINDEX。
     */
    private String taskType;

    /**
     * 状态：PENDING/RUNNING/SUCCEEDED/FAILED/CANCELED。
     */
    private String status;

    /**
     * 已重试次数。
     */
    private Integer retryCount;

    /**
     * 最大重试次数。
     */
    private Integer maxRetry;

    /**
     * 下次重试时间。
     */
    private LocalDateTime nextRetryAt;

    /**
     * 开始时间。
     */
    private LocalDateTime startedAt;

    /**
     * 结束时间。
     */
    private LocalDateTime finishedAt;

    /**
     * 失败原因。
     */
    private String errorMessage;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
