package com.clockshore.aooai.service;

import com.clockshore.aooai.dto.auth.LoginResponse;
import com.clockshore.aooai.entity.AppUser;
import com.clockshore.aooai.mapper.AppUserMapper;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LoginService {

    private static final String ACTIVE_STATUS = "ACTIVE";

    @Resource
    private AppUserMapper appUserMapper;

    @Resource
    private PasswordEncoder passwordEncoder;

    public LoginResponse login(String username, String rawPassword) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(rawPassword)) {
            return LoginResponse.failure("INVALID_LOGIN_PARAM", "用户名和密码不能为空");
        }

        AppUser user = appUserMapper.selectByUsername(username.trim());
        if (user == null || !StringUtils.hasText(user.getPasswordHash())) {
            return LoginResponse.failure("BAD_CREDENTIALS", "用户名或密码错误");
        }

        if (!ACTIVE_STATUS.equals(user.getStatus())) {
            return LoginResponse.failure("ACCOUNT_UNAVAILABLE", "账号当前不可用，请联系管理员");
        }

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            return LoginResponse.failure("BAD_CREDENTIALS", "用户名或密码错误");
        }

        user.setLastLoginAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        appUserMapper.update(user);

        return LoginResponse.success(user);
    }
}
