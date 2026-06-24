package com.clockshore.aooai.mq;

import com.clockshore.aooai.Utility.ToJsonUtil;
import com.clockshore.aooai.entity.LocalMqMessage;
import com.clockshore.aooai.enumType.LocalMqMessageStatus;
import com.clockshore.aooai.mapper.LocalMqMessageMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class ChatMessageMqProducer {

    @Resource
    private LocalMqMessageMapper localMqMessageMapper;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private ToJsonUtil toJsonUtil;

    //persist 持久化
    @Transactional
    public void publishMessagePersist(ChatMessageEvent event) {
        if (localMqMessageMapper.countByEventKey(event.getMessageKey()) > 0) {
            return;
        }
        String payload = toJsonUtil.toJson(event);
        LocalMqMessage localMqMessage = LocalMqMessage.builder()
                .eventKey("need to persist message" + UUID.randomUUID().toString())
                .topic(ChatMqConfig.CHAT_EXCHANGE)
                .routingKey(ChatMqConfig.CHAT_ROUTING_KEY)
                .payload(payload)
                .status(LocalMqMessageStatus.NEW.getValue())
                .retryCount(0)
                .build();
        localMqMessageMapper.insert(localMqMessage);
    }

    @Transactional
    public void publishEmbeddingTask(Long documentId){

        String payload = toJsonUtil.toJson(documentId);
        LocalMqMessage localMqMessage = LocalMqMessage.builder()
                .eventKey("need to embedding:" + UUID.randomUUID().toString())
                .topic(ChatMqConfig.CHAT_EXCHANGE)
                .routingKey(ChatMqConfig.CHAT_ROUTING_KEY)
                .payload(payload)
                .status(LocalMqMessageStatus.NEW.getValue())
                .retryCount(0)
                .build();
        localMqMessageMapper.insert(localMqMessage);
    }
}
