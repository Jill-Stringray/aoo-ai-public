package com.clockshore.aooai.Entity;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppUser {

    /**
     * 用户ID（主键，自增）
     */
    private Long id;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 余额（单位：分 / 最小货币单位）
     */
    private Long balance;

    /**
     * 密码（建议存 Bcrypt 加密后的值）
     */
    private String password;
}