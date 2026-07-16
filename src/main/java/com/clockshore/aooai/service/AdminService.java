package com.clockshore.aooai.service;

import com.clockshore.aooai.entity.AppUser;
import com.clockshore.aooai.mapper.AppUserMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {

    @Resource
    private AppUserMapper appUserMapper;

    public List<AdminUserVO> listUsers() {
        Map<Long, UserUsageVO> usageByUserId = new HashMap<>();
        for (Map<String, Object> row : appUserMapper.selectUserUsageStats()) {
            Long userId = toLong(row.get("userId"));
            if (userId != null) {
                usageByUserId.put(userId, new UserUsageVO(
                        toLong(row.get("conversationCount")),
                        toLong(row.get("questionCount")),
                        toLong(row.get("promptTokens")),
                        toLong(row.get("completionTokens")),
                        toLong(row.get("totalTokens")),
                        row.get("lastAskAt") == null ? null : String.valueOf(row.get("lastAskAt"))
                ));
            }
        }

        return appUserMapper.selectAll().stream()
                .map(user -> AdminUserVO.of(
                        user,
                        appUserMapper.selectPrimaryRoleCodeByUserId(user.getId()),
                        usageByUserId.getOrDefault(user.getId(), UserUsageVO.empty())
                ))
                .toList();
    }

    @Transactional
    public void updateUserStatus(Long userId, String status) {
        if (userId == null) {
            throw new IllegalArgumentException("userId不能为空");
        }
        String nextStatus = status == null ? "" : status.trim().toUpperCase();
        if (!List.of("ACTIVE", "DISABLED", "LOCKED").contains(nextStatus)) {
            throw new IllegalArgumentException("用户状态不合法");
        }
        if (appUserMapper.updateStatus(userId, nextStatus) == 0) {
            throw new IllegalArgumentException("用户不存在");
        }
    }

    private Long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number number) return number.longValue();
        return Long.parseLong(String.valueOf(value));
    }

    public record AdminUserVO(
            Long id,
            String username,
            String displayName,
            String email,
            String mobile,
            String status,
            String roleCode,
            String lastLoginAt,
            String createdAt,
            UserUsageVO usage
    ) {
        public static AdminUserVO of(AppUser user, String roleCode, UserUsageVO usage) {
            return new AdminUserVO(
                    user.getId(),
                    user.getUsername(),
                    user.getDisplayName(),
                    user.getEmail(),
                    user.getMobile(),
                    user.getStatus(),
                    roleCode == null ? "USER" : roleCode,
                    user.getLastLoginAt() == null ? null : user.getLastLoginAt().toString(),
                    user.getCreatedAt() == null ? null : user.getCreatedAt().toString(),
                    usage
            );
        }
    }

    public record UserUsageVO(
            Long conversationCount,
            Long questionCount,
            Long promptTokens,
            Long completionTokens,
            Long totalTokens,
            String lastAskAt
    ) {
        public static UserUsageVO empty() {
            return new UserUsageVO(0L, 0L, 0L, 0L, 0L, null);
        }
    }
}
