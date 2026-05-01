package com.clockshore.aooai.Service;

import com.clockshore.aooai.Entity.LocalMqMessage;
import com.clockshore.aooai.Mapper.LocalMqMessageMapper;
import com.clockshore.aooai.enums.LocalMqMessageStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LocalMqMessageRelayService {

    private static final Logger log = LoggerFactory.getLogger(LocalMqMessageRelayService.class);

    private final LocalMqMessageMapper localMqMessageMapper;
    private final RabbitTemplate rabbitTemplate;

    public LocalMqMessageRelayService(LocalMqMessageMapper localMqMessageMapper, RabbitTemplate rabbitTemplate) {
        this.localMqMessageMapper = localMqMessageMapper;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedDelay = 5000)
    public void dispatch() {
        List<LocalMqMessage> pendingMessages = localMqMessageMapper.selectForDispatch(50);
        for (LocalMqMessage pendingMessage : pendingMessages) {
            int updated = localMqMessageMapper.tryMarkSending(
                    pendingMessage.getId(),
                    LocalMqMessageStatus.SENDING,
                    LocalMqMessageStatus.NEW,
                    LocalMqMessageStatus.FAILED
            );
            if (updated == 0) {
                continue;
            }
            try {
                rabbitTemplate.convertAndSend(pendingMessage.getTopic(), pendingMessage.getRoutingKey(), pendingMessage.getPayload());
                localMqMessageMapper.markSent(pendingMessage.getId(), LocalMqMessageStatus.SENT);
            } catch (Exception e) {
                log.error("dispatch local mq message failed, id={}", pendingMessage.getId(), e);
                localMqMessageMapper.markFailed(pendingMessage.getId(), LocalMqMessageStatus.FAILED, e.getMessage(), 1);
            }
        }
    }
}
