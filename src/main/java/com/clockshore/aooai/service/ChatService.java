package com.clockshore.aooai.service;


import cn.hutool.core.util.IdUtil;
import com.clockshore.aooai.dto.message.ChatRequest;
import com.clockshore.aooai.entity.ConversationMessage;
import com.clockshore.aooai.enumType.CharacterRole;
import com.clockshore.aooai.mapper.ConversationMapper;
import com.clockshore.aooai.mapper.ConversationMessageMapper;
import com.clockshore.aooai.mq.ChatMessageEvent;
import com.clockshore.aooai.mq.ChatMessageMqProducer;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class ChatService {

    private ChatClient chatClient;

    @Resource
    private KnowledgeRetrievalService knowledgeRetrievalService;

    @Resource
    private RagPromptService ragPromptService;

    @Resource
    private ConversationMessageMapper conversationMessageMapper;

    @Resource
    private ChatMessageMqProducer chatMessageMqProducer;

    public ChatService(ChatClient.Builder builder,
                       ConversationMessageMapper conversationMessageMapper
    ){  this.chatClient = builder.build();
        this.conversationMessageMapper = conversationMessageMapper;
    }

    public String nonStreamChat(ChatRequest chatRequest) {

        //Advisor = ChatClient 的“拦截器 / 增强器 / 插件系统”
        long startedAt = System.currentTimeMillis();
        String reply = chatClient.prompt()
                .messages(buildRagMessages(chatRequest))
                .call()
                .content();
        enqueueAssistantMessage(chatRequest, reply, null, System.currentTimeMillis() - startedAt);
        return reply;
    }

    public Flux<ChatResponse> streamChat(ChatRequest chatRequest){

        StringBuilder replyBuffer = new StringBuilder();
        AtomicReference<Usage> usageRef = new AtomicReference<>();
        long startedAt = System.currentTimeMillis();

        return chatClient.prompt()
                .messages(buildMessages(chatRequest))
                .stream()
                .chatResponse()
                .doOnNext(delta -> {
                    //对输出增量进行保存
                    replyBuffer.append(delta.getResult().getOutput().getText());
                    if(delta.getMetadata() != null && delta.getMetadata().getUsage() != null){
                        //循环检测是否为最后一个数据块，获得使用数据
                        usageRef.set(delta.getMetadata().getUsage());
                    }
                })
                .doOnComplete(() -> enqueueAssistantMessage(
                        chatRequest,
                        replyBuffer.toString(),
                        usageRef.get(),
                        System.currentTimeMillis() - startedAt
                ));
    }

    public void enqueueAssistantMessage(ChatRequest chatRequest, String reply, Usage usage, long latencyMs) {
        if (reply == null || reply.isBlank()) {
            return;
        }
        ChatMessageEvent event = ChatMessageEvent.builder()
                .messageKey(IdUtil.getSnowflakeNextIdStr())
                .conversationId(chatRequest.getConversationId())
                .role(CharacterRole.ASSISTANT.getValue())
                .content(reply)
                .modelName(chatRequest.getModel())
                .promptTokens(usage == null ? 0 : toInteger(usage.getPromptTokens()))
                .completionTokens(usage == null ? 0 : toInteger(usage.getCompletionTokens()))
                .totalTokens(usage == null ? 0 : toInteger(usage.getTotalTokens()))
                .latencyMs(toInteger(latencyMs))
                .build();
        chatMessageMqProducer.publishMessagePersist(event);
    }

    //立即保存不走MQ
    @Transactional
    public Long saveAssistantMessage(ChatRequest chatRequest,String reply){
        ConversationMessage conversationMessage = ConversationMessage.builder()
                .messageKey(IdUtil.getSnowflakeNextIdStr())
                .conversationId(chatRequest.getConversationId())
                .content(reply)
                .role(CharacterRole.ASSISTANT.getValue())
                .promptTokens(0)
                .completionTokens(0)
                .totalTokens(0)
                .build();
        return conversationMessageMapper.insert(conversationMessage);
    }

    public List<Message> buildRagMessages(ChatRequest request) {

        List<Message> messages = new ArrayList<>();

        //基础提示词
        messages.add(
                new SystemMessage("""
                你是一个专业的 AI 助手。
                1.只能依据知识库内容回答问题
                2.如果知识库没有相关信息，请明确说“未找到相关内容”
                3.不允许编造答案
                4.请尽量引用原文信息
                """)
        );

        // 解析当前问题
        //todo 加问题改写
        String question = request.resolveMessage();

        // 检索知识库
        List<Document> docs =
                knowledgeRetrievalService.retrieve(
                        question,
                        5
                );

        StringBuilder ragText = new StringBuilder("以下内容为企业知识库召回结果：");

        for (Document doc : docs) {
            ragText.append(ragPromptService.buildAll(doc))
                    .append("\n\n------------------\n\n");
        }

        messages.add(
                new SystemMessage(ragText.toString())
        );

        // 历史消息
        messages.addAll(buildHistory(request));

        // 加入当前用户问题
        messages.add(new UserMessage(question));

        return messages;
    }

    private Integer toInteger(Number value) {
        if (value == null) {
            return 0;
        }
        if (value.longValue() > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return value.intValue();
    }

    public List<Message> buildMessages(ChatRequest chatRequest){
        //history messages + current message
        //将当前消息加入
        List<Message> messages = new ArrayList<>();
        messages.addAll(buildHistory(chatRequest));
        String currentMessage = chatRequest.resolveMessage();
        if (currentMessage != null && !currentMessage.isBlank()) {
            messages.add(new UserMessage(currentMessage));
        }
        return messages;
    }

    public List<Message> buildHistory(ChatRequest chatRequest){
        //构建历史消息
        //不包含当前消息
        List<Message> history = new ArrayList<>();
        List<ConversationMessage> messagesList = conversationMessageMapper.selectByConversationId(chatRequest.getConversationId());
        for (ConversationMessage msg : messagesList) {
            String role = msg.getRole();
            if (CharacterRole.USER.getValue().equals(role)) {
                history.add(new UserMessage(msg.getContent()));
            } else if (CharacterRole.ASSISTANT.getValue().equals(role)) {
                history.add(new AssistantMessage(msg.getContent()));
            } else if (CharacterRole.SYSTEM.getValue().equals(role)) {
                history.add(new SystemMessage(msg.getContent()));
            }
        }
        return history;
    }

}
