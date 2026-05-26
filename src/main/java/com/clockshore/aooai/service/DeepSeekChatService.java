package com.clockshore.aooai.service;

import com.clockshore.aooai.chat.ChatRequest;
import com.clockshore.aooai.entity.ConversationMessage;
import com.clockshore.aooai.enums.Role;
import com.clockshore.aooai.mapper.ConversationMessageMapper;
import com.clockshore.aooai.mq.ChatMessageEvent;
import com.clockshore.aooai.mq.ChatMessageMqProducer;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Service
public class DeepSeekChatService {

    private final ChatClient chatClient;
    private final ConversationMessageMapper conversationMessageMapper;
    private final SnowflakeService snowflakeService;
    private final ChatMessageMqProducer chatMessageMqProducer;
    private final RagService ragService;

    public DeepSeekChatService(ChatClient.Builder builder,
                               ConversationMessageMapper conversationMessageMapper,
                               SnowflakeService snowflakeService,
                               ChatMessageMqProducer chatMessageMqProducer,
                               RagService ragService) {

        this.chatClient = builder.build();
        this.conversationMessageMapper = conversationMessageMapper;
        this.snowflakeService = snowflakeService;
        this.chatMessageMqProducer = chatMessageMqProducer;
        this.ragService = ragService;
    }


    public String nonStreamChat(ChatRequest chatRequest) {

        //Advisor = ChatClient 的“拦截器 / 增强器 / 插件系统”

        return chatClient.prompt()
                .messages(buildMessages(chatRequest))
                .call()
                .content();
    }

    /*public Flux<String> streamChat(ChatRequest chatRequest){

        StringBuilder replyBuffer = new StringBuilder();

        return chatClient.prompt()
                .messages(buildMessages(chatRequest))
                .stream()
                .content()
                .doOnNext(delta -> {
                    replyBuffer.append(delta);
                })
                .doOnComplete(() -> {
                    String fullReply = replyBuffer.toString();
                    if (!fullReply.isBlank()) {
                        chatMessageMqProducer.publishPersist(ChatMessageEvent.builder()
                                .messageKey(snowflakeService.nextIdStr())
                                .conversationId(chatRequest.getConversationId())
                                .userId(chatRequest.getUserId())
                                .role(Role.ASSISTANT.getValue())
                                .content(fullReply)
                                .build());
                    }
                });
    }*/

    public Flux<ChatResponse> streamChat(ChatRequest chatRequest){

        StringBuilder replyBuffer = new StringBuilder();

        return chatClient.prompt()
                .messages(buildMessages(chatRequest))
                .stream()
                .chatResponse()
                .doOnNext(delta -> {
                    replyBuffer.append(delta);
                    System.out.println(delta);
                })
                .doOnComplete(() -> {
                    String fullReply = replyBuffer.toString();
                    if (!fullReply.isBlank()) {
                        chatMessageMqProducer.publishPersist(ChatMessageEvent.builder()
                                .messageKey(snowflakeService.nextIdStr())
                                .conversationId(chatRequest.getConversationId())
                                .userId(chatRequest.getUserId())
                                .role(Role.ASSISTANT.getValue())
                                .content(fullReply)
                                .build());
                    }
                });
    }

    public void enqueueAssistantMessage(ChatRequest chatRequest, String reply) {
        if (reply == null || reply.isBlank()) {
            return;
        }
        chatMessageMqProducer.publishPersist(ChatMessageEvent.builder()
                .messageKey(snowflakeService.nextIdStr())
                .conversationId(chatRequest.getConversationId())
                .userId(chatRequest.getUserId())
                .role(Role.ASSISTANT.getValue())
                .content(reply)
                .build());
    }

    @Transactional
    public Long saveAssistantMessage(ChatRequest chatRequest,String reply){
        ConversationMessage conversationMessage = ConversationMessage.builder()
                .messageKey(snowflakeService.nextIdStr())
                .conversationId(chatRequest.getConversationId())
                .content(reply)
                .role(Role.ASSISTANT.getValue())
                .build();
        return conversationMessageMapper.insert(conversationMessage);
    }

    public List<Message> buildMessages(ChatRequest chatRequest){
        List<Message> messages = new ArrayList<>();
        String ragContext = ragService.buildContext(chatRequest.resolveMessage());
        if (!ragContext.isBlank()) {
            messages.add(new SystemMessage(ragContext));
        }
        messages.addAll(buildHistory(chatRequest));
        String currentMessage = chatRequest.resolveMessage();
        if (currentMessage != null && !currentMessage.isBlank()) {
            messages.add(new UserMessage(currentMessage));
        }
        return messages;
    }

    public List<Message> buildHistory(ChatRequest chatRequest){
        //构建历史消息
        List<Message> history = new ArrayList<>();
        List<ConversationMessage> messagesList = conversationMessageMapper.selectByConversationId(chatRequest.getConversationId());
        for (ConversationMessage msg : messagesList) {
            String role = msg.getRole();
            if (Role.USER.getValue().equals(role)) {
                history.add(new UserMessage(msg.getContent()));
            } else if (Role.ASSISTANT.getValue().equals(role)) {
                history.add(new AssistantMessage(msg.getContent()));
            } else if (Role.SYSTEM.getValue().equals(role)) {
                history.add(new SystemMessage(msg.getContent()));
            }
        }
        return history;
    }

}
