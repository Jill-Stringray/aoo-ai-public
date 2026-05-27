package com.clockshore.aooai.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserProfileResponse {
    private Long userId;
    private String username;
    private String displayName;
    private String email;
    private String department;
    private String role;
    private Integer knowledgeBaseCount;
    private Integer conversationCount;
    private List<String> recentActivities;
}
