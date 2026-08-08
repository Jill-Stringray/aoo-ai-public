package com.clockshore.aooai.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 公司内部用户实体，对应 MySQL 表 app_user。
 *
 * <p>Entity 层只表达数据库字段结构，不直接承载业务规则；
 * 复杂校验、状态流转和权限判断应放在 Service 层，便于后续维护和测试。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppUser {

    /**
     * 用户ID。
     */
    private Long id;

    /**
     * 部门ID。
     */
    private Long departmentId;

    /**
     * 登录用户名。
     */
    private String username;

    /**
     * 不可逆密码哈希。
     */
    private String passwordHash;

    /**
     * 展示姓名。
     */
    private String displayName;

    /**
     * 公司邮箱。
     */
    private String email;

    /**
     * 手机号。
     */
    private String mobile;

    /**
     * 状态：ACTIVE/LOCKED/DISABLED。
     */
    private String status;

    /**
     * 最后登录时间。
     */
    private LocalDateTime lastLoginAt;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
