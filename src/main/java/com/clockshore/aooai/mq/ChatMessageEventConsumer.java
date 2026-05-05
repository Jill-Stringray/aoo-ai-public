package com.clockshore.aooai.mq;

import com.clockshore.aooai.service.ChatWindowService;
import com.clockshore.aooai.service.DeepSeekChatService;
import com.clockshore.aooai.chat.ChatRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ChatMessageEventConsumer {

    @Resource
    private DeepSeekChatService deepSeekChatService;

    @Resource
    private ChatWindowService chatWindowService;

    @Resource
    private ObjectMapper objectMapper;

    private static final Logger log = LoggerFactory.getLogger(ChatMessageEventConsumer.class);

    @RabbitListener(queues = ChatMqConfig.CHAT_QUEUE)
    public void consume(String payload) {
        ChatMessageEvent event = parseEvent(payload);
        log.info("consume chat message event: conversationId={}, messageId={}, userId={},content={}",
                event.getConversationId(), event.getMessageId(), event.getUserId(), event.getContent());

        ChatRequest chatRequest = chatWindowService.buildChatRequestWithHistory(
                event.getUserId(), event.getConversationId(), event.getMessageId());
        String reply = deepSeekChatService.chat(chatRequest);
        if (!StringUtils.hasText(reply)) {
            reply = "(模型未返回内容)";
        }
        chatWindowService.saveAssistantReply(event.getUserId(), event.getConversationId(), reply);
    }

    private ChatMessageEvent parseEvent(String payload) {
        try {
            return objectMapper.readValue(payload, ChatMessageEvent.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid chat message payload: " + payload, e);
        }
    }
}
