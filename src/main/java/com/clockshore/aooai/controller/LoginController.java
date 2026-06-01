package com.clockshore.aooai.controller;

import com.clockshore.aooai.common.ApiResponse;
import com.clockshore.aooai.dto.auth.LoginRequest;
import com.clockshore.aooai.dto.auth.LoginUserVO;
import com.clockshore.aooai.dto.auth.RegisterRequest;
import com.clockshore.aooai.service.LoginService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class LoginController {

    @Resource
    private LoginService loginService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginUserVO>> login(@Valid @RequestBody LoginRequest request) {
        ApiResponse<LoginUserVO> response = loginService.login(request.getUsername(), request.getPassword());
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<LoginUserVO>> register(@Valid @RequestBody RegisterRequest request) {
        ApiResponse<LoginUserVO> response = loginService.register(request);
        if (response.isSuccess()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }
}
