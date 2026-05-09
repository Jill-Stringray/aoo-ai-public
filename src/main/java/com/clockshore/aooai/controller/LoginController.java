package com.clockshore.aooai.controller;

import com.clockshore.aooai.auth.HttpSessionAuth;
import com.clockshore.aooai.auth.SessionConstants;
import com.clockshore.aooai.dto.UserDto;
import com.clockshore.aooai.service.UserService;
import com.clockshore.aooai.vo.UserVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("")
public class LoginController {

    @Resource
    private UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserVO> me(HttpServletRequest request) {
        Long uid = HttpSessionAuth.currentUserId(request);
        if (uid == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserVO vo = userService.getById(uid);
        if (vo == null) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(vo);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserDto u, HttpServletRequest request) {
        UserVO vo = userService.login(u.getUserName(), u.getPassword());
        if (vo == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "用户名或密码错误"));
        }
        request.getSession(true).setAttribute(SessionConstants.USER_ID, vo.getId());
        return ResponseEntity.ok(vo);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserDto u, HttpServletRequest request) {
        try {
            UserVO vo = userService.register(u.getUserName(), u.getPassword());
            request.getSession(true).setAttribute(SessionConstants.USER_ID, vo.getId());
            return ResponseEntity.ok(vo);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
