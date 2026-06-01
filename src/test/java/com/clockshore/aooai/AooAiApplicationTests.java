package com.clockshore.aooai;

import com.clockshore.aooai.mapper.AppUserMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AooAiApplicationTests {

    @Resource
    private AppUserMapper appUserMapper;

    @Test
    void test() {
        appUserMapper.selectById(1L);

    }

}
