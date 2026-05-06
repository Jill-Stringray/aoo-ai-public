package com.clockshore.aooai.service;

import com.clockshore.aooai.chat.ChatRequest;
import com.clockshore.aooai.entity.Conversation;
import com.clockshore.aooai.entity.ConversationMessage;
import com.clockshore.aooai.enums.ConversationStatus;
import com.clockshore.aooai.enums.Role;
import com.clockshore.aooai.mapper.ConversationMapper;
import com.clockshore.aooai.mapper.ConversationMessageMapper;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Service
public class ChatWindowService {

    @Resource
    private ConversationMapper conversationMapper;

    @Resource
    private SnowflakeService snowflakeService;

    @Resource
    private ConversationMessageMapper conversationMessageMapper;

    @Transactional
    @Validated
    public Conversation saveUserMessage(@NotNull Long uid, @NotNull ChatRequest request) {
        String text = request.resolveMessage();
        if (!StringUtils.hasText(text)) {
            throw new IllegalArgumentException("缺少消息内容");
        }

        Conversation c;
        if (request.getConversationId() != null) {
            c = conversationMapper.selectById(request.getConversationId());
            if (c == null) {
                throw new IllegalArgumentException("conversation 未找到: " + request.getConversationId());
            }
            if (!uid.equals(c.getUserId())) {
                throw new IllegalArgumentException("该 conversation 不属于当前用户");
            }
        } else {
            c = conversationMapper.selectByConversationKey(request.getConversationKey());
            if (request.getConversationKey() == null || c == null) {
                c = Conversation.builder()
                        .conversationKey(snowflakeService.nextIdStr())
                        .userId(uid)
                        .status(ConversationStatus.ACTIVE.getValue())
                        .build();
                conversationMapper.insert(c);
            }
        }

        ConversationMessage message = ConversationMessage.builder()
                .messageKey(snowflakeService.nextIdStr())
                .conversationId(c.getId())
                .content(text.trim())
                .role(Role.USER.getValue())
                .build();

        conversationMessageMapper.insert(message);
        return c;
    }

    public List<ConversationMessage> listMessages(@NotNull Long uid, @NotNull Long conversationId) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new IllegalArgumentException("conversation 未找到: " + conversationId);
        }
        if (!uid.equals(conversation.getUserId())) {
            throw new IllegalArgumentException("该 conversation 不属于当前用户");
        }
        return conversationMessageMapper.selectByConversationId(conversationId);
    }
}
