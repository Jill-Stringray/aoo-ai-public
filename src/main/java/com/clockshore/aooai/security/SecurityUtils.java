package com.clockshore.aooai.security;


import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 获取当前登录用户
     */
    public static LoginUser getLoginUser() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof LoginUser loginUser) {
            return loginUser;
        }

        return null;
    }

    /**
     * 获取当前用户ID
     */
    public static Long getUserId() {

        LoginUser loginUser = getLoginUser();

        return loginUser == null ? null : loginUser.getUserId();
    }

}
