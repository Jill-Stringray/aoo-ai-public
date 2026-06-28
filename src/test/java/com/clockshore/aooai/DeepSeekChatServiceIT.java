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
                .message("Va-11 HALL-A 和 cyberpunkRed 有什么关系？")
                .userId(1L)
                .model("deepseek")
                .stream(true)
                .build();

        System.out.println(chatService.buildRagMessages(c));
        System.out.println(chatService.nonStreamChat(c));
        //todo 检索质量低

        System.out.println("\n\n*******************ChatTest*******************\n\n");
    }
}
