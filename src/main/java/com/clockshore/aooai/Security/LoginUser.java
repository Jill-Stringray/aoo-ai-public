package com.clockshore.aooai.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * Spring Security 登录用户信息。
 *
 * <p>
 * 不对应数据库表，只负责保存当前认证用户的信息。
 * 由 JWT Filter 创建并放入 SecurityContextHolder。
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser implements UserDetails {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 登录用户名
     */
    private String username;

    /**
     * BCrypt 加密后的密码
     */
    private String password;

    /**
     * 用户权限
     */
    @Builder.Default
    private Collection<? extends GrantedAuthority> authorities =
            java.util.Collections.emptyList();


    /**
     * 账户是否未过期
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * 账户是否未锁定
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * 密码是否未过期
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * 账户是否可用
     */
    @Override
    public boolean isEnabled() {
        return true;
    }
}
