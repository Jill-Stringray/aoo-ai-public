package com.clockshore.aooai.service;

import com.clockshore.aooai.mapper.AppUserMapper;
import jakarta.annotation.Resource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class LoginService {

    @Resource
    private AppUserMapper appUserMapper;

    @Resource
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    public xxx login(String username,String rawPassword){
        String encoded = appUserMapper.selectByUsername(username).getPasswordHash();
        if(encoded == null){
            return "a null return error";
        }else if(bCryptPasswordEncoder.matches(rawPassword, encoded)){
            return "a yes return";
        }else{
            return "a no return";
        }

    }
}
