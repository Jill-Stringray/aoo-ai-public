package com.clockshore.aooai.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 本地 MQ 消息实体，对应 MySQL 表 local_mq_message。
 *
 * <p>Entity 层只表达数据库字段结构，不直接承载业务规则；
 * 复杂校验、状态流转和投递重试策略应放在 Service 层，便于后续维护和测试。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocalMqMessage {

    /**
     * 消息ID。
     */
    private Long id;

    /**
     * 事件唯一键，用于保证消息写入幂等。
     */
    private String eventKey;

    /**
     * MQ 交换机或主题名称。
     */
    private String topic;

    /**
     * MQ 路由键。
     */
    private String routingKey;

    /**
     * 消息载荷 JSON。
     */
    private String payload;

    /**
     * 状态：NEW/SENDING/SENT/FAILED。
     */
    private String status;

    /**
     * 已重试次数。
     */
    private Integer retryCount;

    /**
     * 下次重试时间。
     */
    private LocalDateTime nextRetryAt;

    /**
     * 失败原因。
     */
    private String failReason;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
