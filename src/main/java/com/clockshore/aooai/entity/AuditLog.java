package com.clockshore.aooai.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 审计日志实体，对应 MySQL 表 audit_log。
 *
 * <p>Entity 层只表达数据库字段结构，不直接承载业务规则；
 * 复杂校验、状态流转和权限判断应放在 Service 层，便于后续维护和测试。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    /**
     * 审计ID。
     */
    private Long id;

    /**
     * 操作者用户ID。
     */
    private Long userId;

    /**
     * 动作，如 KB_CREATE/DOC_UPLOAD/QA_ASK。
     */
    private String action;

    /**
     * 资源类型。
     */
    private String resourceType;

    /**
     * 资源ID。
     */
    private String resourceId;

    /**
     * 请求链路ID。
     */
    private String requestId;

    /**
     * 公司内网客户端 IP。
     */
    private String ipAddress;

    /**
     * 客户端 UA。
     */
    private String userAgent;

    /**
     * 审计详情 JSON。
     */
    private String detail;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
}
