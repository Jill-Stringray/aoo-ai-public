package com.clockshore.aooai.service;

import com.clockshore.aooai.entity.AppUser;
import com.clockshore.aooai.mapper.UserMapper;
import com.clockshore.aooai.vo.UserVO;
import jakarta.annotation.Resource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserService {
    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private UserMapper userMapper;

    public UserVO getById(Long id) {
        if (id == null) {
            return null;
        }
        AppUser u = userMapper.selectById(id);
        if (u == null) {
            return null;
        }
        return UserVO.builder()
                .id(u.getId())
                .userName(u.getUserName())
                .balance(u.getBalance())
                .build();
    }

    public UserVO login(String userName, String rawPassword) {
        if (!StringUtils.hasText(userName) || !StringUtils.hasText(rawPassword)) {
            return null;
        }
        AppUser u = userMapper.selectByUserName(userName.trim());
        if (u == null || u.getPassword() == null || !passwordEncoder.matches(rawPassword, u.getPassword())) {
            return null;
        }
        return UserVO.builder()
                .id(u.getId())
                .userName(u.getUserName())
                .balance(u.getBalance())
                .build();
    }

    @Transactional
    public UserVO register(String userName, String rawPassword) {
        if (!StringUtils.hasText(userName) || !StringUtils.hasText(rawPassword)) {
            throw new IllegalArgumentException("用户名和密码不能为空");
        }
        String name = userName.trim();
        if (userMapper.selectByUserName(name) != null) {
            throw new IllegalArgumentException("用户名已存在");
        }
        String password = passwordEncoder.encode(rawPassword);
        AppUser u = AppUser.builder().userName(name).balance(50L).password(password).build();
        userMapper.insert(u);
        AppUser created = userMapper.selectById(u.getId());
        return UserVO.builder()
                .id(created.getId())
                .userName(created.getUserName())
                .balance(created.getBalance())
                .build();
    }

}
