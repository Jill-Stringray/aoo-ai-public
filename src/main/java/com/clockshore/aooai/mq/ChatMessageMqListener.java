package com.clockshore.aooai.mq;

import com.clockshore.aooai.entity.ConversationMessage;
import com.clockshore.aooai.mapper.ConversationMessageMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ChatMessageMqListener {

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private ConversationMessageMapper conversationMessageMapper;

    @Transactional
    @RabbitListener(queues = ChatMqConfig.CHAT_QUEUE)
    public void consume(String payload) {
        ChatMessageEvent event = parse(payload);
        ConversationMessage conversationMessage = ConversationMessage.builder()
                .messageKey(event.getMessageKey())
                .conversationId(event.getConversationId())
                .content(event.getContent())
                .role(event.getRole())
                .build();
        conversationMessageMapper.insert(conversationMessage);
    }

    private ChatMessageEvent parse(String payload) {
        try {
            return objectMapper.readValue(payload, ChatMessageEvent.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("parse chat message event failed", e);
        }
    }
}
