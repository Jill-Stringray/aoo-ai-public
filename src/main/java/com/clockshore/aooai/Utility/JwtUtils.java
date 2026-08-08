package com.clockshore.aooai.Utility;

import com.clockshore.aooai.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtUtils {

    private final JwtProperties properties;

    /**
     * 获取签名密钥
     */
    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(
                properties.getSecret().getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     *  生成token
     */

    public String generateToken(Long userId) {

        Date now = new Date();

        Date expireTime = new Date(
                now.getTime() + properties.getExpire()
        );

        return Jwts.builder()
                // 用户ID
                .subject(userId.toString())
                // 签发时间
                .issuedAt(now)
                // 过期时间
                .expiration(expireTime)
                // 签名
                .signWith(getSecretKey())
                .compact();
    }

    /**
     * 解析claims
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 获取 userId
     */
    public Long getUserId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

}
