package com.clockshore.aooai.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 问答反馈实体，对应 MySQL 表 qa_feedback。
 *
 * <p>Entity 层只表达数据库字段结构，不直接承载业务规则；
 * 复杂校验、状态流转和权限判断应放在 Service 层，便于后续维护和测试。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QaFeedback {

    /**
     * 反馈ID。
     */
    private Long id;

    /**
     * 被反馈的回答消息ID。
     */
    private Long messageId;

    /**
     * 反馈用户ID。
     */
    private Long userId;

    /**
     * 评分：1=赞，-1=踩，0=中性。
     */
    private Byte rating;

    /**
     * 反馈原因。
     */
    private String reason;

    /**
     * 员工建议答案或补充说明。
     */
    private String suggestion;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
}
