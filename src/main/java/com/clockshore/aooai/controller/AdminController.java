package com.clockshore.aooai.controller;

import com.clockshore.aooai.common.ApiResponse;
import com.clockshore.aooai.service.AdminService;
import com.clockshore.aooai.service.RagDebugService;
import com.clockshore.aooai.service.SystemConfigService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    @Resource
    private AdminService adminService;

    @Resource
    private SystemConfigService systemConfigService;

    @Resource
    private RagDebugService ragDebugService;

    @GetMapping("/users")
    public ApiResponse<List<AdminService.AdminUserVO>> users() {
        return ApiResponse.success("ADMIN_USERS_FETCHED", "系统用户列表已获取", adminService.listUsers());
    }

    @PutMapping("/users/{userId}/status")
    public ApiResponse<Void> updateUserStatus(
            @PathVariable("userId") Long userId,
            @RequestBody Map<String, String> request) {
        adminService.updateUserStatus(userId, request.get("status"));
        return ApiResponse.success("ADMIN_USER_STATUS_UPDATED", "用户状态已更新", null);
    }

    @GetMapping("/config")
    public ApiResponse<SystemConfigService.SystemConfig> config() {
        return ApiResponse.success("ADMIN_CONFIG_FETCHED", "系统配置已获取", systemConfigService.getConfig());
    }

    @PutMapping("/config")
    public ApiResponse<SystemConfigService.SystemConfig> updateConfig(
            @RequestBody SystemConfigService.SystemConfig request) {
        return ApiResponse.success(
                "ADMIN_CONFIG_UPDATED",
                "系统配置已保存",
                systemConfigService.updateConfig(request)
        );
    }

    @PostMapping("/rag/debug")
    public ApiResponse<RagDebugService.RagDebugResult> debugRag(
            @RequestBody RagDebugService.RagDebugRequest request) {
        return ApiResponse.success(
                "ADMIN_RAG_DEBUG_FINISHED",
                "RAG检索调试完成",
                ragDebugService.debug(request)
        );
    }
}
