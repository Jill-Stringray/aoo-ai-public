package com.clockshore.aooai.mq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class ChatMessageEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public ChatMessageEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(ChatMessageEvent event) {
        rabbitTemplate.convertAndSend(ChatMqConfig.CHAT_EXCHANGE, ChatMqConfig.CHAT_ROUTING_KEY, event);
    }
}
