package com.clockshore.aooai.mq;

import com.clockshore.aooai.entity.LocalMqMessage;
import com.clockshore.aooai.enumType.LocalMqMessageStatus;
import com.clockshore.aooai.mapper.LocalMqMessageMapper;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 本地 MQ 消息投递器：先落库，再异步投递 RabbitMQ，最后回写投递状态。
 */
@Component
public class LocalMqMessageDispatcher {

    private static final int DEFAULT_BATCH_SIZE = 50;
    private static final int MAX_FAIL_REASON_LENGTH = 2048;

    @Resource
    private LocalMqMessageMapper localMqMessageMapper;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Scheduled(fixedDelay = 3000)
    public void dispatch() {
        localMqMessageMapper.recoverTimeoutSending(LocalMqMessageStatus.FAILED, LocalMqMessageStatus.SENDING, 5);
        List<LocalMqMessage> messages = localMqMessageMapper.selectForDispatch(DEFAULT_BATCH_SIZE);
        for (LocalMqMessage message : messages) {
            dispatch(message);
        }
    }

    private void dispatch(LocalMqMessage message) {
        int marked = localMqMessageMapper.tryMarkSending(
                message.getId(),
                LocalMqMessageStatus.SENDING,
                LocalMqMessageStatus.NEW,
                LocalMqMessageStatus.FAILED
        );
        if (marked == 0) {
            return;
        }

        try {
            rabbitTemplate.convertAndSend(message.getTopic(), message.getRoutingKey(), message.getPayload());
            localMqMessageMapper.markSent(message.getId(), LocalMqMessageStatus.SENT);
        } catch (Exception e) {
            localMqMessageMapper.markFailed(
                    message.getId(),
                    LocalMqMessageStatus.FAILED,
                    truncateFailReason(e),
                    nextRetryMinutes(message.getRetryCount())
            );
        }
    }

    private int nextRetryMinutes(Integer retryCount) {
        int currentRetryCount = retryCount == null ? 0 : retryCount;
        return Math.min(30, Math.max(1, currentRetryCount + 1));
    }

    private String truncateFailReason(Exception e) {
        String message = e.getMessage();
        String failReason = message == null ? e.getClass().getName() : message;
        if (failReason.length() <= MAX_FAIL_REASON_LENGTH) {
            return failReason;
        }
        return failReason.substring(0, MAX_FAIL_REASON_LENGTH);
    }
}
