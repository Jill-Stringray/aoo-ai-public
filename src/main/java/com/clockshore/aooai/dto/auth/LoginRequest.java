package com.clockshore.aooai.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求 DTO。
 *
 * <p>Controller 只接收前端需要提交的字段，不直接暴露数据库实体，
 * 这样可以避免前端误传或后端误返回 passwordHash 等敏感字段。</p>
 */
@Data
public class LoginRequest {

    /**
     * 登录用户名。
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 用户输入的明文密码，只用于本次 BCrypt 校验，绝不落库或返回给前端。
     */
    @NotBlank(message = "密码不能为空")
    private String password;
}
