package com.clockshore.aooai.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 事务外盒事件实体，对应 MySQL 表 outbox_event。
 *
 * <p>Entity 层只表达数据库字段结构，不直接承载业务规则；
 * 复杂校验、状态流转和权限判断应放在 Service 层，便于后续维护和测试。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {

    /**
     * 事件ID。
     */
    private Long id;

    /**
     * 事件唯一键，建议应用层保证幂等。
     */
    private String eventKey;

    /**
     * 事件类型，如 DOCUMENT_UPLOADED。
     */
    private String eventType;

    /**
     * 聚合类型。
     */
    private String aggregateType;

    /**
     * 聚合ID。
     */
    private String aggregateId;

    /**
     * 事件载荷 JSON。
     */
    private String payload;

    /**
     * 状态：NEW/PUBLISHED/FAILED。
     */
    private String status;

    /**
     * 重试次数。
     */
    private Integer retryCount;

    /**
     * 下次重试时间。
     */
    private LocalDateTime nextRetryAt;

    /**
     * 最后一次失败原因。
     */
    private String lastError;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
