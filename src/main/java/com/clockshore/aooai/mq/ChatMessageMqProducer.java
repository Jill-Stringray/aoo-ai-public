package com.clockshore.aooai.mq;

import com.clockshore.aooai.Utility.JsonUtil;
import com.clockshore.aooai.entity.LocalMqMessage;
import com.clockshore.aooai.enumType.LocalMqMessageStatus;
import com.clockshore.aooai.mapper.LocalMqMessageMapper;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ChatMessageMqProducer {

    @Resource
    private LocalMqMessageMapper localMqMessageMapper;

    @Resource
    private JsonUtil jsonUtil;

    //persist 持久化
    @Transactional
    public void publishMessagePersist(ChatMessageEvent event) {
        String eventKey = "chat-message:" + event.getMessageKey();
        if (localMqMessageMapper.countByEventKey(eventKey) > 0) {
            return;
        }
        String payload = jsonUtil.toJson(event);
        LocalMqMessage localMqMessage = LocalMqMessage.builder()
                .eventKey(eventKey)
                .topic(ChatMqConfig.CHAT_EXCHANGE)
                .routingKey(ChatMqConfig.CHAT_ROUTING_KEY)
                .payload(payload)
                .status(LocalMqMessageStatus.NEW.getValue())
                .retryCount(0)
                .nextRetryAt(LocalDateTime.now())
                .build();
        localMqMessageMapper.insert(localMqMessage);
    }

    @Transactional
    public LocalMqMessage publishEmbeddingTask(Long documentId){

        String payload = jsonUtil.toJson(documentId);
        LocalMqMessage localMqMessage = LocalMqMessage.builder()
                .eventKey("embedding:" + documentId)
                .topic(ChatMqConfig.EMBEDDING_EXCHANGE)
                .routingKey(ChatMqConfig.EMBEDDING_ROUTING_KEY)
                .payload(payload)
                .status(LocalMqMessageStatus.NEW.getValue())
                .retryCount(0)
                .nextRetryAt(LocalDateTime.now())
                .build();
        localMqMessageMapper.insert(localMqMessage);
        return localMqMessage;
    }
}
