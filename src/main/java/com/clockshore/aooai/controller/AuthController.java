package com.clockshore.aooai.controller;

import com.clockshore.aooai.dto.LoginRequest;
import com.clockshore.aooai.dto.LoginResponse;
import com.clockshore.aooai.dto.UserProfileResponse;
import com.clockshore.aooai.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/profile")
    public UserProfileResponse profile(@RequestParam(defaultValue = "student") String username) {
        return authService.profile(username);
    }
}
