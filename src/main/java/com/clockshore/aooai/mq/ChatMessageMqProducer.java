package com.clockshore.aooai.mq;

import com.clockshore.aooai.entity.LocalMqMessage;
import com.clockshore.aooai.enums.LocalMqMessageStatus;
import com.clockshore.aooai.mapper.LocalMqMessageMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ChatMessageMqProducer {

    @Resource
    private LocalMqMessageMapper localMqMessageMapper;

    @Resource
    private ObjectMapper objectMapper;

    @Transactional
    public void publishPersist(ChatMessageEvent event) {
        String payload = toJson(event);
        LocalMqMessage localMqMessage = LocalMqMessage.builder()
                .eventKey(event.getMessageKey())
                .topic(ChatMqConfig.CHAT_EXCHANGE)
                .routingKey(ChatMqConfig.CHAT_ROUTING_KEY)
                .payload(payload)
                .status(LocalMqMessageStatus.NEW.getValue())
                .retryCount(0)
                .build();
        localMqMessageMapper.insert(localMqMessage);
    }

    private String toJson(ChatMessageEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("serialize chat message event failed", e);
        }
    }
}
