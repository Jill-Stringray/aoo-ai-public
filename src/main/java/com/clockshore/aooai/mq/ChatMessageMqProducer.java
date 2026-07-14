package com.clockshore.aooai.mq;

import com.clockshore.aooai.Utility.ToJsonUtil;
import com.clockshore.aooai.entity.LocalMqMessage;
import com.clockshore.aooai.enumType.LocalMqMessageStatus;
import com.clockshore.aooai.mapper.LocalMqMessageMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ChatMessageMqProducer {

    @Resource
    private LocalMqMessageMapper localMqMessageMapper;

    @Resource
    private ToJsonUtil toJsonUtil;

    //persist 持久化
    @Transactional
    public void publishMessagePersist(ChatMessageEvent event) {
        String eventKey = "chat-message:" + event.getMessageKey();
        if (localMqMessageMapper.countByEventKey(eventKey) > 0) {
            return;
        }
        String payload = toJsonUtil.toJson(event);
        LocalMqMessage localMqMessage = LocalMqMessage.builder()
                .eventKey(eventKey)
                .topic(ChatMqConfig.CHAT_EXCHANGE)
                .routingKey(ChatMqConfig.CHAT_ROUTING_KEY)
                .payload(payload)
                .status(LocalMqMessageStatus.NEW.getValue())
                .retryCount(0)
                .build();
        localMqMessageMapper.insert(localMqMessage);
    }

    @Transactional
    public LocalMqMessage publishEmbeddingTask(Long documentId){

        String payload = toJsonUtil.toJson(documentId);
        LocalMqMessage localMqMessage = LocalMqMessage.builder()
                .eventKey("embedding:" + documentId)
                .topic(ChatMqConfig.EMBEDDING_EXCHANGE)
                .routingKey(ChatMqConfig.EMBEDDING_ROUTING_KEY)
                .payload(payload)
                .status(LocalMqMessageStatus.NEW.getValue())
                .retryCount(0)
                .build();
        localMqMessageMapper.insert(localMqMessage);
        return localMqMessage;
    }
}
