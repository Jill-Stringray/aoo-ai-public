package com.clockshore.aooai;

import com.clockshore.aooai.dto.message.ChatRequest;
import com.clockshore.aooai.service.ChatService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = AooAiApplication.class)
class DeepSeekChatServiceIT {

    @Resource
    private ChatService chatService;

    @Test
    void chatTest(){
        System.out.println("\n\n*******************ChatTest*******************\n\n");

        ChatRequest c = ChatRequest.builder()
                .conversationId(1L)
                .conversationKey("test")
                .message("介绍 jill stingray 在这里面的角色是什么？")
                .userId(1L)
                .model("deepseek")
                .stream(true)
                .build();

        System.out.println(chatService.buildRagMessages(c));
        System.out.println(chatService.nonStreamChat(c));
        System.out.println();

        System.out.println("\n\n*******************ChatTest*******************\n\n");
    }
}
