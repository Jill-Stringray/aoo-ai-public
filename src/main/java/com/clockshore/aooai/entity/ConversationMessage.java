package com.clockshore.aooai.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会话消息实体，对应 MySQL 表 conversation_message。
 *
 * <p>表达数据库字段结构，承载一定的业务规则,但是Spring AI核心链路还是使用它给定的Message类
 * 复杂校验、状态流转和权限判断应放在 Service 层，便于后续维护和测试。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMessage {

    /**
     * 消息ID。
     */
    private Long id;

    /**
     * 会话ID。
     */
    private Long conversationId;

    /**
     * 消息业务唯一键。
     */
    private String messageKey;

    /**
     * 父消息ID，用于多轮追踪。
     */
    private Long parentMessageId;

    /**
     * 角色：SYSTEM/USER/ASSISTANT/TOOL。
     */
    private String role;

    /**
     * 消息内容。
     */
    private String content;

    /**
     * 模型名称。
     */
    private String modelName;

    /**
     * 输入 token。
     */
    private Integer promptTokens;

    /**
     * 输出 token。
     */
    private Integer completionTokens;

    /**
     * 总 token。
     */
    private Integer totalTokens;

    /**
     * 响应耗时毫秒。
     */
    private Integer latencyMs;

    /**
     * 模型结束原因。
     */
    private String finishReason;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
}
