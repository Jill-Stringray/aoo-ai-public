package com.clockshore.aooai;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AooAiApplicationTests {

    /*@Resource
    private DeepSeekChatService deepSeekChatService;


    @Test
    void testStreamChat() {

        ChatRequest request = new ChatRequest();
        request.setConversationId(1L);
        request.setUserId(1L);
        request.setMessage("你好");

        deepSeekChatService.streamChat(request)
                .doOnNext(response -> {
                    System.out.println("=========" + response.getResult().getOutput().getText());
                })
                .blockLast();
    }*/

}
