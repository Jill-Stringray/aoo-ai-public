package com.clockshore.aooai.dto.auth;

import com.clockshore.aooai.entity.AppUser;
import lombok.Builder;
import lombok.Data;

/**
 * 登录接口统一返回对象。
 *
 * <p>这里就是原代码中 {@code xxx} 应该替换成的返回类型。企业项目里通常不建议
 * Controller/Service 直接返回字符串，因为前端很难稳定判断业务状态，也容易泄露内部原因。</p>
 */
@Data
@Builder
public class LoginResponse {

    /**
     * 本次登录是否成功。
     */
    private boolean success;

    /**
     * 面向程序判断的业务码，例如 LOGIN_SUCCESS、BAD_CREDENTIALS。
     */
    private String code;

    /**
     * 面向用户展示的提示文案。
     */
    private String message;

    /**
     * 用户ID。
     */
    private Long userId;

    /**
     * 登录用户名。
     */
    private String username;

    /**
     * 展示姓名。
     */
    private String displayName;

    /**
     * 公司邮箱。
     */
    private String email;

    /**
     * 账号状态：ACTIVE/LOCKED/DISABLED。
     */
    private String status;

    /**
     * 当前前端演示页面需要展示角色，正式 RBAC 接入后可改成角色列表。
     */
    private String role;

    public static LoginResponse success(AppUser user) {
        return LoginResponse.builder()
                .success(true)
                .code("LOGIN_SUCCESS")
                .message("登录成功")
                .userId(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .email(user.getEmail())
                .status(user.getStatus())
                .role("USER")
                .build();
    }

    public static LoginResponse failure(String code, String message) {
        return LoginResponse.builder()
                .success(false)
                .code(code)
                .message(message)
                .role("GUEST")
                .build();
    }
}
