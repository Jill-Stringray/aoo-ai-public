package com.clockshore.aooai.security;

import com.clockshore.aooai.entity.AppUser;
import com.clockshore.aooai.mapper.AppUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 负责加载当前登录用户信息。
 *
 * <p>
 * JWT 中只保存 userId，
 * 每次请求通过 userId 查询用户并转换为 Spring Security 使用的 LoginUser。
 * </p>
 */


import java.util.Collections;

@Service
@RequiredArgsConstructor
public class LoginUserService {

    private final AppUserMapper appUserMapper;

    /**
     * 根据用户ID加载登录用户
     */
    public LoginUser loadByUserId(Long userId) {

        AppUser user = appUserMapper.selectById(userId);

        if (user == null) {
            return null;
        }

        return LoginUser.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(Collections.emptyList())
                .build();
    }

}