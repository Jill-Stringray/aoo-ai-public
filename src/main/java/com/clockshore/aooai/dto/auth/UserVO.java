package com.clockshore.aooai.dto.auth;

import com.clockshore.aooai.entity.AppUser;
import lombok.Builder;
import lombok.Data;
import org.springframework.util.StringUtils;

/**
 * 登录成功后返回给前端的用户视图对象。
 *
 * <p>VO 只描述页面真正需要展示或保存的用户信息，不包含 passwordHash 等敏感数据库字段。</p>
 */
@Data
@Builder
public class UserVO {

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
     * 当前前端页面需要展示的主角色；后续可扩展为角色列表。
     */
    private String role;

    public static UserVO from(AppUser user) {
        return from(user, null);
    }

    public static UserVO from(AppUser user, String roleCode) {
        return UserVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .email(user.getEmail())
                .status(user.getStatus())
                .role(StringUtils.hasText(roleCode) ? roleCode : "USER")
                .build();
    }
}
