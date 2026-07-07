package com.clockshore.aooai.security;

import com.clockshore.aooai.Utility.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;



@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final LoginUserService loginUserService;

    /**
     * Filter负责：
     * <p>1.Token 存不存在，Token 格式对不对，Token 能不能解析</p>
     * <p>2.把用户信息放进 SecurityContextHolder，之后通过SecurityUtils进行调用</p>
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        System.out.println("******JwtAuthenticationFilter working******");

        // 获取请求头
        String authorization = request.getHeader("Authorization");

        // 没有 Token，直接放行
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 去掉 Bearer 前缀
        String token = authorization.substring(7);

        try {

            // 解析 JWT，获取 userId
            Long userId = jwtUtils.getUserId(token);

            // 查询用户
            LoginUser loginUser = loginUserService.loadByUserId(userId);

            if (loginUser != null) {
                // 当前登录用户
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                loginUser,
                                null,
                                loginUser.getAuthorities()
                        );

                // 放进SecurityContextHolder
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

        } catch (Exception e) {
            System.out.println("JWT token 校验失败: " + e.getMessage());
        }

        // 6. 放行请求
        filterChain.doFilter(request, response);
    }

}