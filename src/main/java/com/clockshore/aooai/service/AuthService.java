package com.clockshore.aooai.service;

import com.clockshore.aooai.dto.LoginRequest;
import com.clockshore.aooai.dto.LoginResponse;
import com.clockshore.aooai.dto.UserProfileResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    public LoginResponse login(LoginRequest request) {
        return LoginResponse.builder()
                .userId(1001L)
                .username(request.getUsername())
                .displayName("企业用户-" + request.getUsername())
                .role("USER")
                .token("demo-token-" + UUID.randomUUID())
                .build();
    }

    public UserProfileResponse profile(String username) {
        return UserProfileResponse.builder()
                .userId(1001L)
                .username(username)
                .displayName("企业用户-" + username)
                .email(username + "@example.com")
                .department("数字化平台主管理部")
                .role("USER")
                .knowledgeBaseCount(3)
                .conversationCount(18)
                .recentActivities(List.of("创建知识库：售后知识库", "录入文档：售后排障资料-SOP-v1.pdf", "发起问答：E02报警排查流程"))
                .build();
    }
}
