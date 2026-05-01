package com.clockshore.aooai.Service;

import com.clockshore.aooai.Entity.Conversation;
import com.clockshore.aooai.Entity.ConversationMessage;
import com.clockshore.aooai.Mapper.ConversationMapper;
import com.clockshore.aooai.Mapper.ConversationMessageMapper;
import com.clockshore.aooai.Mapper.LocalMqMessageMapper;
import com.clockshore.aooai.enums.ConversationStatus;
import com.clockshore.aooai.Entity.LocalMqMessage;
import com.clockshore.aooai.mq.ChatMessageEvent;
import com.clockshore.aooai.mq.ChatMqConfig;
import com.clockshore.aooai.enums.Role;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;


@Service
public class ChatWindowService {

    @Resource
    private ConversationMapper conversationMapper;

    @Resource
    private SnowflakeService snowflakeService;

    @Resource
    private ConversationMessageMapper conversationMessageMapper;

    @Resource
    private LocalMqMessageMapper localMqMessageMapper;

    @Resource
    private ObjectMapper objectMapper;


    @Transactional
    @Validated
    public Conversation sendMessage(@NotNull Long uid, Long conversationId,@NotBlank String content){
        //在 Chat 类系统里，应该只有一个核心动作：发消息
        //sendMessage同时兼容第一条和第n条消息
        //service 核心业务 + 强一致数据写入，不做耗时/不稳定操作,ai回复时长...

        Conversation conversation;
        if (conversationId == null) {
            conversation = createConversation(uid);
            conversationId = conversation.getId();
        } else {
            conversation = conversationMapper.selectById(conversationId);
            if (conversation == null) {
                throw new IllegalArgumentException("conversation 未找到: " + conversationId);
            }
            if (!uid.equals(conversation.getUserId())) {
                throw new IllegalArgumentException("该conversation不属于用户");
            }
        }

        ConversationMessage message = buildConversationMessage(conversationId,content,Role.USER.getValue());
        conversationMessageMapper.insert(message);
        saveLocalMqMessage(new ChatMessageEvent(conversationId, message.getId(), uid, content));

        return conversation;
    }
    private void saveLocalMqMessage(ChatMessageEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            LocalMqMessage localMqMessage = LocalMqMessage.builder()
                    .topic(ChatMqConfig.CHAT_EXCHANGE)
                    .routingKey(ChatMqConfig.CHAT_ROUTING_KEY)
                    .payload(payload)
                    .status("NEW")
                    .retryCount(0)
                    .build();
            localMqMessageMapper.insert(localMqMessage);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("序列化MQ消息失败", e);
        }
    }

    @Validated
    private Conversation buildConversation(@NotNull Long uid){
        String conversationKey = snowflakeService.nextIdStr();

        return Conversation.builder()
                .userId(uid)
                .conversationKey(conversationKey)
                .status(ConversationStatus.ACTIVE.getValue())
                .build();
    }

    @Validated
    private ConversationMessage buildConversationMessage(@NotNull Long conversationId,@NotBlank String content,@NotNull String role){

        return ConversationMessage.builder()
                .id(snowflakeService.nextId())
                .conversationId(conversationId)
                .role(role)
                .content(content)
                .build();
    }

    @Transactional
    public Conversation createConversation(@NotNull Long uid) {
        Conversation cvs = buildConversation(uid);
        conversationMapper.insert(cvs);
        return cvs;
    }


}
