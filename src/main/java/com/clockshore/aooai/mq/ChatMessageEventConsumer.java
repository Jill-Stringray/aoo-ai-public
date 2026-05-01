package com.clockshore.aooai.mq;

import com.clockshore.aooai.Mapper.ConversationMessageMapper;
import com.clockshore.aooai.Service.DeepSeekChatService;
import com.clockshore.aooai.chat.ChatRequest;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ChatMessageEventConsumer {

    @Resource
    private DeepSeekChatService deepSeekChatService;

    @Resource
    private ConversationMessageMapper conversationMessageMapper;

    private static final Logger log = LoggerFactory.getLogger(ChatMessageEventConsumer.class);

    @RabbitListener(queues = ChatMqConfig.CHAT_QUEUE)
    public void consume(ChatMessageEvent event) {
        log.info("consume chat message event: conversationId={}, messageId={}, userId={},content={}",
                event.getConversationId(), event.getMessageId(), event.getUserId(), event.getContent());

        //请求ai回复
        deepSeekChatService.chat(
                ChatRequest.builder()
                        .message(event.getContent())
                        .messages(conversationMessageMapper.toChatRequestMessages(
                                conversationMessageMapper.selectByConversationId(event.getConversationId())))
                        .build()
        );


    }
}
