package com.clockshore.aooai.service;

import cn.hutool.core.util.IdUtil;
import com.clockshore.aooai.dto.message.ChatMessage;
import com.clockshore.aooai.dto.message.ChatRetrievalSource;
import com.clockshore.aooai.dto.message.ConversationVO;
import com.clockshore.aooai.entity.Conversation;
import com.clockshore.aooai.entity.ConversationMessage;
import com.clockshore.aooai.entity.MessageRetrievalSource;
import com.clockshore.aooai.enumType.ConversationStatus;
import com.clockshore.aooai.mapper.ConversationMapper;
import com.clockshore.aooai.mapper.ConversationMessageMapper;
import com.clockshore.aooai.mapper.MessageRetrievalSourceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationMapper conversationMapper;

    private final ConversationMessageMapper conversationMessageMapper;

    private final MessageRetrievalSourceMapper messageRetrievalSourceMapper;

    public Long createConversation(Long userId) {
        Conversation conversation = new Conversation();
        conversation.setConversationKey(IdUtil.getSnowflakeNextIdStr());
        conversation.setUserId(userId);
        conversation.setTitle("新对话");
        conversation.setStatus(ConversationStatus.ACTIVE.getValue());

        conversationMapper.insert(conversation);

        return conversation.getId();
    }

    public List<ConversationVO> fetchConversationList(Long userId) {
        List<Conversation> conversations = conversationMapper.selectByUserId(userId);
        List<ConversationVO> conversationVOS = new ArrayList<>();
        for (Conversation conversation : conversations) {
            conversationVOS.add(ConversationVO.from(conversation));
        }

        return conversationVOS;
    }

    public List<ChatMessage> fetchConversationMessages(Long userId, Long conversationId, Integer limit, Long beforeId) {
        limit = Math.min(limit, 50);
        List<ConversationMessage> conversationMessages =
                conversationMessageMapper.selectByConversationIdAndUserId(conversationId, userId, beforeId, limit);
        Map<String, List<ChatRetrievalSource>> sourcesByMessageKey = fetchSourcesByMessageKey(conversationMessages);

        List<ChatMessage> messages = new ArrayList<>();
        for (int i = conversationMessages.size() - 1; i >= 0; i--) {
            ConversationMessage conversationMessage = conversationMessages.get(i);
            ChatMessage chatMessage = conversationMessageMapper.toChatMessage(conversationMessage);
            if (chatMessage != null) {
                chatMessage.setSources(sourcesByMessageKey.getOrDefault(conversationMessage.getMessageKey(), List.of()));
                messages.add(chatMessage);
            }
        }
        return messages;
    }

    public void renameConversation(Long userId, Long conversationId, String title) {
        String normalizedTitle = title == null ? "" : title.trim();
        if (normalizedTitle.isEmpty()) {
            throw new IllegalArgumentException("会话标题不能为空");
        }
        if (normalizedTitle.length() > 255) {
            normalizedTitle = normalizedTitle.substring(0, 255);
        }

        int updated = conversationMapper.updateTitleForUser(conversationId, userId, normalizedTitle);
        if (updated == 0) {
            throw new IllegalArgumentException("会话不存在或无权修改");
        }
    }

    public void deleteConversation(Long userId, Long conversationId) {
        int deleted = conversationMapper.deleteByIdAndUserId(conversationId, userId);
        if (deleted == 0) {
            throw new IllegalArgumentException("会话不存在或无权删除");
        }
    }

    private Map<String, List<ChatRetrievalSource>> fetchSourcesByMessageKey(List<ConversationMessage> messages) {
        List<String> messageKeys = messages.stream()
                .map(ConversationMessage::getMessageKey)
                .filter(Objects::nonNull)
                .filter(messageKey -> !messageKey.isBlank())
                .distinct()
                .toList();

        if (messageKeys.isEmpty()) {
            return Map.of();
        }

        return messageRetrievalSourceMapper.selectByMessageKeys(messageKeys).stream()
                .collect(Collectors.groupingBy(
                        MessageRetrievalSource::getMessageKey,
                        Collectors.mapping(ChatRetrievalSource::from, Collectors.toList())
                ));
    }
}
