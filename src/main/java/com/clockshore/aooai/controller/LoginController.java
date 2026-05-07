package com.clockshore.aooai.controller;

import com.clockshore.aooai.dto.UserDto;
import com.clockshore.aooai.entity.AppUser;
import com.clockshore.aooai.mapper.AppUserMapper;
import com.clockshore.aooai.vo.UserVO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("")
public class LoginController {
    @Resource
    private AppUserMapper appUserMapper;

    @PostMapping("/login")
    public UserVO login(@RequestBody UserDto u){
        return appUserMapper.login(u);
    }
}
