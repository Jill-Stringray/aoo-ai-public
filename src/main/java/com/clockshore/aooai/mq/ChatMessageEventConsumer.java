package com.clockshore.aooai.mq;

import com.clockshore.aooai.Mapper.ConversationMessageMapper;
import com.clockshore.aooai.Service.SnowflakeService;
import com.clockshore.aooai.Service.DeepSeekChatService;
import com.clockshore.aooai.Entity.ConversationMessage;
import com.clockshore.aooai.chat.ChatRequest;
import com.clockshore.aooai.enums.Role;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ChatMessageEventConsumer {

    @Resource
    private DeepSeekChatService deepSeekChatService;

    @Resource
    private ConversationMessageMapper conversationMessageMapper;

    @Resource
    private SnowflakeService snowflakeService;

    private static final Logger log = LoggerFactory.getLogger(ChatMessageEventConsumer.class);

    @RabbitListener(queues = ChatMqConfig.CHAT_QUEUE)
    public void consume(ChatMessageEvent event) {
        log.info("consume chat message event: conversationId={}, messageId={}, userId={},content={}",
                event.getConversationId(), event.getMessageId(), event.getUserId(), event.getContent());

        // 基于完整上下文请求 AI 回复
        String assistantReply = deepSeekChatService.chat(
                ChatRequest.builder()
                        .messages(conversationMessageMapper.toChatRequestMessages(
                                conversationMessageMapper.selectByConversationId(event.getConversationId())))
                        .build()
        );

        // 落库 assistant 消息，保持会话上下文持续可用
        ConversationMessage assistantMessage = ConversationMessage.builder()
                .id(snowflakeService.nextId())
                .conversationId(event.getConversationId())
                .role(Role.ASSISTANT.getValue())
                .content(assistantReply)
                .build();
        conversationMessageMapper.insert(assistantMessage);


    }
}
