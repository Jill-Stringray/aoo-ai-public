package com.clockshore.aooai.mq;

import com.clockshore.aooai.Utility.JsonUtil;
import com.clockshore.aooai.entity.LocalMqMessage;
import com.clockshore.aooai.enumType.LocalMqMessageStatus;
import com.clockshore.aooai.mapper.LocalMqMessageMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RetrievalSourceMqProducer {

    @Resource
    private LocalMqMessageMapper localMqMessageMapper;

    @Resource
    private JsonUtil jsonUtil;

    /**
     * 投递消息召回来源持久化：先落库，再由 LocalMqMessageDispatcher 投递到 RabbitMQ。
     */
    @Transactional
    public void publishRetrievalSourcePersist(RetrievalSourceEvent event) {
        String payload = jsonUtil.toJson(event);

        LocalMqMessage localMqMessage = LocalMqMessage.builder()
                .eventKey("retrieval-source:" + event.getMessageKey())
                .topic(ChatMqConfig.RETRIEVAL_EXCHANGE)
                .routingKey(ChatMqConfig.RETRIEVAL_ROUTING_KEY)
                .payload(payload)
                .status(LocalMqMessageStatus.NEW.getValue())
                .retryCount(0)
                .build();
        localMqMessageMapper.insert(localMqMessage);
    }
}
