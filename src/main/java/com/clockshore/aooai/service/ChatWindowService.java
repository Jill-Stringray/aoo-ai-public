package com.clockshore.aooai.service;

import com.clockshore.aooai.chat.ChatRequest;
import com.clockshore.aooai.entity.Conversation;
import com.clockshore.aooai.entity.ConversationMessage;
import com.clockshore.aooai.enums.ConversationStatus;
import com.clockshore.aooai.enums.Role;
import com.clockshore.aooai.mapper.ConversationMapper;
import com.clockshore.aooai.mapper.ConversationMessageMapper;
import com.clockshore.aooai.mq.ChatMessageEvent;
import com.clockshore.aooai.mq.ChatMessageMqProducer;
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
    private ChatMessageMqProducer chatMessageMqProducer;

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
            c = requireOwnedActiveConversation(uid, request.getConversationId());
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

        String messageKey = snowflakeService.nextIdStr();
        chatMessageMqProducer.publishPersist(ChatMessageEvent.builder()
                .messageKey(messageKey)
                .conversationId(c.getId())
                .userId(uid)
                .role(Role.USER.getValue())
                .content(text.trim())
                .build());
        return c;
    }

    public List<ConversationMessage> listMessages(@NotNull Long uid, @NotNull Long conversationId) {
        requireOwnedActiveConversation(uid, conversationId);
        return conversationMessageMapper.selectByConversationId(conversationId);
    }

    public List<Conversation> listConversationsForUser(@NotNull Long uid) {
        return conversationMapper.selectByUserId(uid);
    }

    @Transactional
    public Conversation createConversation(@NotNull Long uid, String title) {
        Conversation c = Conversation.builder()
                .conversationKey(snowflakeService.nextIdStr())
                .userId(uid)
                .title(StringUtils.hasText(title) ? title.trim() : null)
                .status(ConversationStatus.ACTIVE.getValue())
                .build();
        conversationMapper.insert(c);
        return conversationMapper.selectById(c.getId());
    }

    public Conversation getConversation(@NotNull Long uid, @NotNull Long conversationId) {
        return requireOwnedActiveConversation(uid, conversationId);
    }

    @Transactional
    public Conversation updateConversationTitle(@NotNull Long uid, @NotNull Long conversationId, String title) {
        Conversation c = requireOwnedActiveConversation(uid, conversationId);
        c.setTitle(StringUtils.hasText(title) ? title.trim() : null);
        conversationMapper.updateById(c);
        return conversationMapper.selectById(conversationId);
    }

    @Transactional
    public void deleteConversation(@NotNull Long uid, @NotNull Long conversationId) {
        Conversation c = conversationMapper.selectById(conversationId);
        if (c == null) {
            throw new IllegalArgumentException("conversation 未找到: " + conversationId);
        }
        if (!uid.equals(c.getUserId())) {
            throw new IllegalArgumentException("该 conversation 不属于当前用户");
        }
        int n = conversationMapper.updateStatusForUser(
                conversationId,
                uid,
                ConversationStatus.DELETED.getValue());
        if (n == 0) {
            throw new IllegalArgumentException("删除会话失败");
        }
    }

    private Conversation requireOwnedActiveConversation(@NotNull Long uid, @NotNull Long conversationId) {
        Conversation c = conversationMapper.selectById(conversationId);
        if (c == null) {
            throw new IllegalArgumentException("conversation 未找到: " + conversationId);
        }
        if (!uid.equals(c.getUserId())) {
            throw new IllegalArgumentException("该 conversation 不属于当前用户");
        }
        if (!isActiveConversationStatus(c.getStatus())) {
            throw new IllegalArgumentException("该会话已删除或不可用");
        }
        return c;
    }

    private boolean isActiveConversationStatus(String status) {
        return status != null && "ACTIVE".equalsIgnoreCase(status.trim());
    }
}
