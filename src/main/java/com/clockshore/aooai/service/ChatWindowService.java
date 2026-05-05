package com.clockshore.aooai.service;

import com.clockshore.aooai.entity.Conversation;
import com.clockshore.aooai.entity.ConversationMessage;
import com.clockshore.aooai.enums.Role;
import com.clockshore.aooai.mapper.ConversationMapper;
import com.clockshore.aooai.chat.ChatRequest;
import com.clockshore.aooai.enums.ConversationStatus;
import com.clockshore.aooai.mapper.ConversationMessageMapper;
import com.clockshore.aooai.mq.ChatMessageEvent;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;


@Service
public class ChatWindowService {

    @Resource
    private ConversationMapper conversationMapper;

    @Resource
    private SnowflakeService snowflakeService;
    @Autowired
    private ConversationMessageMapper conversationMessageMapper;


    @Transactional
    @Validated
    public Conversation saveMessage(@NotNull Long uid, @NotNull ChatRequest request) {
        //目前还未引入用户系统，所以暂时不对传入的uid参数进行处理

        Conversation c = conversationMapper.selectByConversationKey(request.getConversationKey());
        //新对话或未找到
        if(request.getConversationKey() == null || c == null){
            //此处重写了c
                c = Conversation.builder()
                    .conversationKey(snowflakeService.nextIdStr())
                    .userId(1L)
                    .status(ConversationStatus.ACTIVE.getValue())
                    .build();
            conversationMapper.insert(c);
        }
        Long conversationId = c.getId();
        ConversationMessage message = ConversationMessage.builder()
                .messageKey(snowflakeService.nextIdStr())
                .conversationId(conversationId)
                .content(request.getMessage())
                .role(Role.USER.getValue())
                .build();

        conversationMessageMapper.insert(message);

        return c;
    }


    /**
    public record UserMessagePersisted(Conversation conversation, ConversationMessage message, Long userId) {
    }



    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private LocalMqMessageMapper localMqMessageMapper;



    @Resource
    private ConversationMessageMapper conversationMessageMapper;

    @Transactional
    @Validated
    public Conversation saveMessage(@NotNull Long uid, @NotNull ChatRequest request) {
        UserMessagePersisted saved = persistUserMessage(uid, request);
        saveLocalMqMessage(new ChatMessageEvent(
                saved.conversation().getId(),
                saved.message().getId(),
                uid,
                saved.message().getContent()));
        return saved.conversation();
    }

    /**
     * 仅落库用户消息，不投递 MQ（供 SSE 流式路径在连接内直接调模型并落库 assistant）。

    @Transactional
    @Validated
    public UserMessagePersisted saveUserMessageWithoutMq(@NotNull Long uid, @NotNull ChatRequest request) {
        return persistUserMessage(uid, request);
    }

    private UserMessagePersisted persistUserMessage(@NotNull Long uid, @NotNull ChatRequest request) {
        Long conversationId = request.getConversationId();
        String content = request.resolveMessage();
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("缺少消息内容");
        }

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

        ConversationMessage message = buildConversationMessage(conversationId, content, Role.USER.getValue());
        conversationMessageMapper.insert(message);
        return new UserMessagePersisted(conversation, message, uid);
    }

    private void saveLocalMqMessage(ChatMessageEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            LocalMqMessage localMqMessage = LocalMqMessage.builder()
                    .eventKey(buildEventKey(event))
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

    private String buildEventKey(ChatMessageEvent event) {
        return "chat:send:" + event.getMessageId();
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

    /**
     * 为 MQ 消费构建带完整上下文的请求：按时间顺序包含截止本条用户消息（含）之前的所有库内消息。

    public ChatRequest buildChatRequestWithHistory(@NotNull Long uid, @NotNull Long conversationId,
                                                   @NotNull Long upToUserMessageId) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new IllegalArgumentException("conversation 未找到: " + conversationId);
        }
        if (!uid.equals(conversation.getUserId())) {
            throw new IllegalArgumentException("该conversation不属于用户");
        }

        List<ConversationMessage> ordered = conversationMessageMapper.selectByConversationId(conversationId);
        List<ConversationMessage> context = new ArrayList<>();
        boolean found = false;
        for (ConversationMessage m : ordered) {
            context.add(m);
            if (m.getId().equals(upToUserMessageId)) {
                found = true;
                break;
            }
        }
        if (!found) {
            throw new IllegalArgumentException("触发消息未找到: " + upToUserMessageId);
        }

        List<ChatMessage> chatMessages = conversationMessageMapper.toChatMessages(context);
        return ChatRequest.builder()
                .conversationId(conversationId)
                .messages(chatMessages)
                .stream(false)
                .build();
    }
        */
}
