package com.clockshore.aooai.factory;

import cn.hutool.core.util.IdUtil;
import com.clockshore.aooai.enumType.CharacterRole;
import com.clockshore.aooai.mq.ChatMessageEvent;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.stereotype.Component;

@Component
public class ChatMessageEventFactory {

    public ChatMessageEvent buildUserMessage(
            Long conversationId,
            String content
    ) {
        return ChatMessageEvent.builder()
                .messageKey(IdUtil.getSnowflakeNextIdStr())
                .conversationId(conversationId)
                .role(CharacterRole.USER.getValue())
                .content(content)
                .build();
    }

    public ChatMessageEvent buildAssistantMessage(
            Long conversationId,
            String messageKey,
            String content,
            String modelName,
            Usage usage,
            long latencyMs
    ) {
        return ChatMessageEvent.builder()
                .messageKey(messageKey)
                .conversationId(conversationId)
                .role(CharacterRole.ASSISTANT.getValue())
                .content(content)
                .modelName(modelName)
                .promptTokens(usage == null ? 0 : toInteger(usage.getPromptTokens()))
                .completionTokens(usage == null ? 0 : toInteger(usage.getCompletionTokens()))
                .totalTokens(usage == null ? 0 : toInteger(usage.getTotalTokens()))
                .latencyMs(toInteger(latencyMs))
                .build();
    }

    public ChatMessageEvent buildAssistantMessage(
            Long conversationId,
            Long parentMessageId,
            String messageKey,
            String content,
            String modelName,
            Usage usage,
            long latencyMs
    ) {
        ChatMessageEvent event = buildAssistantMessage(conversationId, messageKey, content, modelName, usage, latencyMs);
        event.setParentMessageId(parentMessageId);
        return event;
    }

    private Integer toInteger(Number number) {
        return number == null ? 0 : number.intValue();
    }
}
