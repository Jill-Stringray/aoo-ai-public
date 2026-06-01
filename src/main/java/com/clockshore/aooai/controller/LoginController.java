package com.clockshore.aooai.controller;

import com.clockshore.aooai.service.LoginService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("/auth")
public class LoginController {

    @Resource
    private LoginService loginService;

    @PostMapping("/login")
    public xxx
}
