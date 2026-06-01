package com.clockshore.aooai.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求 DTO。
 *
 * <p>当前注册接口只服务后端写库联调，因此仅要求 username/password 必填；
 * displayName 为空时由 Service 层回退为用户名，避免数据库 NOT NULL 字段写入失败。</p>
 */
@Data
public class RegisterRequest {

    /**
     * 登录用户名。
     */
    @NotBlank(message = "用户名不能为空")
    @Size(max = 64, message = "用户名长度不能超过64个字符")
    private String username;

    /**
     * 明文密码，只用于生成不可逆哈希，不会落库或返回。
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 72, message = "密码长度必须在6到72个字符之间")
    private String password;

    /**
     * 展示姓名；为空时默认使用用户名。
     */
    @Size(max = 128, message = "展示姓名长度不能超过128个字符")
    private String displayName;

    /**
     * 公司邮箱。
     */
    @Size(max = 128, message = "邮箱长度不能超过128个字符")
    private String email;

    /**
     * 手机号。
     */
    @Size(max = 32, message = "手机号长度不能超过32个字符")
    private String mobile;

    /**
     * 部门ID，可为空。
     */
    private Long departmentId;
}
