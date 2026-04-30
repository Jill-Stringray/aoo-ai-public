package com.clockshore.aooai.mq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ChatMessageEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ChatMessageEventConsumer.class);

    @RabbitListener(queues = ChatMqConfig.CHAT_QUEUE)
    public void consume(ChatMessageEvent event) {
        log.info("consume chat message event: conversationId={}, messageId={}, userId={}",
                event.getConversationId(), event.getMessageId(), event.getUserId());
    }
}
