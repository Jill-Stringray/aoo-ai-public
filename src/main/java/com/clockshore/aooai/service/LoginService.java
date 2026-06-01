package com.clockshore.aooai.service;

import com.clockshore.aooai.common.ApiResponse;
import com.clockshore.aooai.dto.auth.LoginUserVO;
import com.clockshore.aooai.dto.auth.RegisterRequest;
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

    public ApiResponse<LoginUserVO> register(RegisterRequest request) {
        String username = request.getUsername().trim();
        if (appUserMapper.selectByUsername(username) != null) {
            return ApiResponse.failure("USERNAME_EXISTS", "用户名已存在");
        }

        LocalDateTime now = LocalDateTime.now();
        AppUser user = AppUser.builder()
                .departmentId(request.getDepartmentId())
                .username(username)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .displayName(defaultDisplayName(request.getDisplayName(), username))
                .email(trimToNull(request.getEmail()))
                .mobile(trimToNull(request.getMobile()))
                .status(ACTIVE_STATUS)
                .createdAt(now)
                .updatedAt(now)
                .build();

        appUserMapper.insert(user);
        return ApiResponse.success("REGISTER_SUCCESS", "注册成功", LoginUserVO.from(user));
    }

    public ApiResponse<LoginUserVO> login(String username, String rawPassword) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(rawPassword)) {
            return ApiResponse.failure("INVALID_LOGIN_PARAM", "用户名和密码不能为空");
        }
        AppUser user = appUserMapper.selectByUsername(username.trim());
        if (user == null || !StringUtils.hasText(user.getPasswordHash())) {
            return ApiResponse.failure("BAD_CREDENTIALS", "用户名或密码错误");
        }

        if (!ACTIVE_STATUS.equals(user.getStatus())) {
            return ApiResponse.failure("ACCOUNT_UNAVAILABLE", "账号当前不可用，请联系管理员");
        }

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            return ApiResponse.failure("BAD_CREDENTIALS", "用户名或密码错误");
        }

        user.setLastLoginAt(LocalDateTime.now());
        appUserMapper.update(user);
        return ApiResponse.success("LOGIN_SUCCESS", "登录成功", LoginUserVO.from(user));
    }


    private String defaultDisplayName(String displayName, String username) {
        if (StringUtils.hasText(displayName)) {
            return displayName.trim();
        }
        return username;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
