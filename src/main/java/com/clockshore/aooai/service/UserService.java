package com.clockshore.aooai.service;

import com.clockshore.aooai.entity.AppUser;
import com.clockshore.aooai.mapper.UserMapper;
import jakarta.annotation.Resource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private UserMapper userMapper;

    @Transactional
    public Long register(String userName,String rawPassword){
        String password = passwordEncoder.encode(rawPassword);
        AppUser u = AppUser.builder().userName(userName).balance(50L).password(password).build();
        userMapper.insert(u);
        return u.getId();
    }

}
