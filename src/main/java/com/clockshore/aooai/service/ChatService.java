package com.clockshore.aooai.service;


import cn.hutool.core.util.IdUtil;
import com.clockshore.aooai.dto.message.ChatRequest;
import com.clockshore.aooai.entity.ConversationMessage;
import com.clockshore.aooai.enumType.CharacterRole;
import com.clockshore.aooai.factory.ChatMessageEventFactory;
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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;


@Service
public class ChatService {

    private ChatClient chatClient;

    @Resource
    private SseService sseService;

    @Resource
    private KnowledgeRetrievalService knowledgeRetrievalService;

    @Resource
    private RagPromptService ragPromptService;

    @Resource
    private ConversationMessageMapper conversationMessageMapper;

    @Resource
    private ChatMessageMqProducer chatMessageMqProducer;

    @Resource
    private ChatMessageEventFactory chatMessageEventFactory;

    public ChatService(ChatClient.Builder builder,
                       ConversationMessageMapper conversationMessageMapper
    ){  this.chatClient = builder.build();
        this.conversationMessageMapper = conversationMessageMapper;
    }

    public String nonStreamChat(ChatRequest chatRequest) {

        //Advisor = ChatClient 的“拦截器 / 增强器 / 插件系统”
        long startedAt = System.currentTimeMillis();
        ChatResponse response = chatClient.prompt()
                .messages(buildRagMessages(chatRequest))
                .call()
                .chatResponse();

        if (response == null || response.getResult() == null) {
            throw new IllegalStateException("模型未返回有效响应");
        }

        String reply = response.getResult().getOutput().getText();
        Usage usage = response.getMetadata().getUsage();
        enqueueAssistantMessage(chatRequest.getConversationId(), reply, chatRequest.getModel(),usage,System.currentTimeMillis() - startedAt);
        return reply;
    }



    public Flux<ChatResponse> streamChat(ChatRequest chatRequest){


        // 1. 保存用户消息
        enqueueUserMessage(chatRequest.getConversationId(),chatRequest.getMessage());


        StringBuilder replyBuffer = new StringBuilder();
        AtomicReference<Usage> usageRef = new AtomicReference<>();
        long startedAt = System.currentTimeMillis();

        return chatClient.prompt()
                .messages(buildRagMessages(chatRequest))
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
                        chatRequest.getConversationId(),
                        replyBuffer.toString(),
                        chatRequest.getModel(),
                        usageRef.get(),
                        System.currentTimeMillis() - startedAt
                ));
    }

    /**
     * MQ投递AI助手消息持久化
     *
     */
    public void enqueueAssistantMessage(Long conversationId,
                                        String reply,
                                        String model,
                                        Usage usage,
                                        long latencyMs
    ) {
        if (reply == null || reply.isBlank()) {
            return;
        }
        chatMessageMqProducer.publishMessagePersist(
                chatMessageEventFactory.buildAssistantMessage(
                        conversationId,
                        reply,
                        model,
                        usage,
                        latencyMs
                )
        );
    }

    /**
     * MQ投递用户消息持久化
     */
    public void enqueueUserMessage(
            Long conversationId,
            String content
    ) {
        if (content == null || content.isBlank()) {
            return;
        }

        chatMessageMqProducer.publishMessagePersist(
                chatMessageEventFactory.buildUserMessage(
                        conversationId,
                        content
                )
        );
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
                        你是一名专业的企业 AI 助手。
                        请按照以下规则回答用户问题：
                        【信息来源优先级】
                
                        1. 如果用户的问题涉及当前聊天记录，例如：
                           - 我刚才说了什么
                           - 我叫什么
                           - 你之前回答了什么
                           - 我们聊了哪些内容
                           等与当前会话相关的问题，
                           应仅依据聊天历史回答，不需要查询知识库，也不要回答"未找到相关内容"。
                
                        2. 如果用户询问企业业务、产品、制度、文档等知识库相关内容：
                           - 应优先依据知识库内容回答。
                           - 优先综合多个召回片段，而不是仅使用单个片段。
                           - 可以对知识库内容进行归纳、整理和等价表达。
                           - 不得编造事实，不得引入知识库之外的信息，不得推断知识库未明确说明的内容。
                
                        3. 如果知识库未检索到相关内容：
                           - 明确回复："知识库中未找到相关内容。"
                           - 不要编造知识库不存在的信息。
                
                        【回答格式】
                
                        当依据知识库回答时，建议采用以下结构：
                
                        1. 简要结论
                        2. 根据知识库整理的要点
                        3. 必要时引用相关原文
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
