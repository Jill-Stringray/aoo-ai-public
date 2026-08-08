package com.clockshore.aooai;

import com.clockshore.aooai.Utility.JwtUtils;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Paths;

@SpringBootTest(classes = AooAiApplication.class)
public class JwtIT {

    @Autowired
    private JwtUtils jwtUtils;


    @Test
    void JwtGenerate() throws Exception {

        System.out.println("********"+jwtUtils.generateToken(1L));

    }
}
