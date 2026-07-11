package com.clockshore.aooai.mq;

import com.clockshore.aooai.enumType.CharacterRole;
import com.clockshore.aooai.service.KnowledgeService;
import com.clockshore.aooai.entity.ConversationMessage;
import com.clockshore.aooai.mapper.ConversationMapper;
import com.clockshore.aooai.mapper.ConversationMessageMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Component
public class ChatMessageMqListener {

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private KnowledgeService knowledgeService;

    @Resource
    private ConversationMessageMapper conversationMessageMapper;

    @Resource
    private ConversationMapper conversationMapper;

    @Transactional
    @RabbitListener(queues = ChatMqConfig.CHAT_QUEUE)
    public void chatMessageConsume(String payload) {
        ChatMessageEvent event = parse(payload);
        ConversationMessage conversationMessage = ConversationMessage.builder()
                .conversationId(event.getConversationId())
                .messageKey(event.getMessageKey())
                .parentMessageId(event.getParentMessageId())
                .role(event.getRole())
                .content(event.getContent())
                .modelName(event.getModelName())
                .promptTokens(defaultZero(event.getPromptTokens()))
                .completionTokens(defaultZero(event.getCompletionTokens()))
                .totalTokens(defaultZero(event.getTotalTokens()))
                .latencyMs(event.getLatencyMs())
                .finishReason(event.getFinishReason())
                .build();
        conversationMessageMapper.insert(conversationMessage);
        if (CharacterRole.USER.getValue().equals(event.getRole())) {
            conversationMapper.touchAfterUserMessage(event.getConversationId(), buildConversationTitle(event.getContent()));
        }
    }

    @RabbitListener(queues = ChatMqConfig.EMBEDDING_QUEUE)
    public void embeddingConsume(String payload) {
        Long documentId = parseDocumentId(payload);
        knowledgeService.indexNewChunks(documentId);
    }

    private Long parseDocumentId(String payload) {
        try {
            return objectMapper.readValue(payload, Long.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("parse embedding task payload failed", e);
        }
    }

    private Integer defaultZero(Integer value) {
        return value == null ? 0 : value;
    }

    private String buildConversationTitle(String content) {
        if (content == null || content.isBlank()) {
            return "新对话";
        }
        String title = content.trim().replaceAll("\\s+", " ");
        return title.length() > 30 ? title.substring(0, 30) : title;
    }

    private ChatMessageEvent parse(String payload) {
        try {
            return objectMapper.readValue(payload, ChatMessageEvent.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("parse chat message event failed", e);
        }
    }

}
