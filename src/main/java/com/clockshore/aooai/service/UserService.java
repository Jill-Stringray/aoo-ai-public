package com.clockshore.aooai.service;

import jakarta.annotation.Resource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    @Resource
    private PasswordEncoder passwordEncoder;

    public String register(String userName,String rawPassword){
        String password = passwordEncoder.encode(rawPassword);

    }
}
