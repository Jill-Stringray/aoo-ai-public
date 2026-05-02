package com.clockshore.aooai.Service;

import com.clockshore.aooai.Entity.Conversation;
import com.clockshore.aooai.Entity.ConversationMessage;
import com.clockshore.aooai.Mapper.ConversationMapper;
import com.clockshore.aooai.Mapper.ConversationMessageMapper;
import com.clockshore.aooai.chat.ChatRequest;
import com.clockshore.aooai.enums.ConversationStatus;
import com.clockshore.aooai.enums.Role;
import com.clockshore.aooai.mq.ChatMessageEvent;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;


@Service
public class ChatWindowService {

    @Resource
    private ConversationMapper conversationMapper;
    private DeepSeekChatService deepSeekChatService;

    @Resource
    private SnowflakeService snowflakeService;

    @Resource
    private ConversationMessageMapper conversationMessageMapper;

    @Transactional
    @Validated
    public Conversation  saveMessage(@NotNull Long uid, @NotNull ChatRequest request){
        //在 Chat 类系统里，应该只有一个核心动作：发消息
        //sendMessage同时兼容第一条和第n条消息
        //保存user消息
        Long conversationId = request.getConversationId();
        String content = request.getMessage();

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
        String reply = deepSeekChatService.chat(request);
        saveAssistantReply(uid, conversation.getId(), reply);

        return conversation;
    }

    @Transactional
    public ConversationMessage saveAssistantReply(@NotNull Long uid, @NotNull Long conversationId, @NotBlank String reply) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new IllegalArgumentException("conversation 未找到: " + conversationId);
        }
        if (!uid.equals(conversation.getUserId())) {
            throw new IllegalArgumentException("该conversation不属于用户");
        }

        ConversationMessage assistantMessage = buildConversationMessage(conversationId, reply, Role.ASSISTANT.getValue());
        conversationMessageMapper.insert(assistantMessage);
        return assistantMessage;
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

    public List<ConversationMessage> listMessages(@NotNull Long uid, @NotNull Long conversationId) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new IllegalArgumentException("conversation 未找到: " + conversationId);
        }
        if (!uid.equals(conversation.getUserId())) {
            throw new IllegalArgumentException("该conversation不属于用户");
        }
        return conversationMessageMapper.selectByConversationId(conversationId);
    }


}
