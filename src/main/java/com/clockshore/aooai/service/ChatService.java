package com.clockshore.aooai.service;


import cn.hutool.core.util.IdUtil;
import com.clockshore.aooai.dto.message.ChatRequest;
import com.clockshore.aooai.dto.mq.RetrievalSourceDTO;
import com.clockshore.aooai.entity.Conversation;
import com.clockshore.aooai.entity.ConversationMessage;
import com.clockshore.aooai.entity.MessageRetrievalSource;
import com.clockshore.aooai.enumType.CharacterRole;
import com.clockshore.aooai.enumType.ConversationStatus;
import com.clockshore.aooai.factory.ChatMessageEventFactory;
import com.clockshore.aooai.mapper.ConversationMapper;
import com.clockshore.aooai.mapper.ConversationMessageMapper;
import com.clockshore.aooai.mq.ChatMessageMqProducer;
import com.clockshore.aooai.mq.RetrievalSourceEvent;
import com.clockshore.aooai.mq.RetrievalSourceMqProducer;
import com.clockshore.aooai.security.SecurityUtils;
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
import java.util.Map;
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

    @Resource
    private ConversationMapper conversationMapper;

    @Resource
    private RetrievalSourceMqProducer retrievalSourceMqProducer;

    @Resource
    private ConversationService conversationService;

    public ChatService(ChatClient.Builder builder,
                       ConversationMessageMapper conversationMessageMapper
    ){  this.chatClient = builder.build();
        this.conversationMessageMapper = conversationMessageMapper;
    }

    public String nonStreamChat(ChatRequest chatRequest) {

        //Advisor = ChatClient 的“拦截器 / 增强器 / 插件系统”
        ensureConversationReady(chatRequest);
        long startedAt = System.currentTimeMillis();
        //生成雪花key
        String messageKey = IdUtil.getSnowflakeNextIdStr();

        ChatResponse response = chatClient.prompt()
                .messages(buildRagMessages(chatRequest,messageKey))
                .call()
                .chatResponse();

        if (response == null || response.getResult() == null) {
            throw new IllegalStateException("模型未返回有效响应");
        }

        String reply = response.getResult().getOutput().getText();
        Usage usage = response.getMetadata().getUsage();
        enqueueAssistantMessage(
                chatRequest.getConversationId(),
                messageKey,
                reply,
                chatRequest.getModel(),
                usage,
                System.currentTimeMillis() - startedAt);
        return reply;
    }



    public Flux<ChatResponse> streamChat(ChatRequest chatRequest){


        ensureConversationReady(chatRequest);


        // 1. 保存用户消息
        enqueueUserMessage(chatRequest.getConversationId(),chatRequest.getMessage());

        //生成雪花
        String messageKey = IdUtil.getSnowflakeNextIdStr();


        StringBuilder replyBuffer = new StringBuilder();
        AtomicReference<Usage> usageRef = new AtomicReference<>();
        long startedAt = System.currentTimeMillis();

        return chatClient.prompt()
                .messages(buildRagMessages(chatRequest,messageKey))
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
                        messageKey,
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
                                        String messageKey,
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
                        messageKey,
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

    /**
     * MQ投递消息召回来源持久化
     */
    public void enqueueRetrievalSource(
            String messageKey,
            List<RetrievalSourceDTO> sources
    ) {

        if (messageKey == null || sources == null || sources.isEmpty()) {
            return;
        }

        retrievalSourceMqProducer.publishRetrievalSourcePersist(
                RetrievalSourceEvent
                        .builder()
                        .messageKey(messageKey)
                        .sources(sources).build()
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

    public List<Message> buildRagMessages(ChatRequest request,String messageKey) {

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
            
                       - 应优先依据知识库召回内容回答。
                       - 优先综合多个召回片段，而不是仅使用单个片段。
                       - 可以对知识库内容进行归纳、整理和总结。
                       - 不得编造事实，不得引入知识库之外的信息。
                       - 不得推断知识库未明确说明的内容。
            
            
                    3. 如果知识库未检索到相关内容：
            
                       - 明确回复："知识库中未找到相关内容。"
                       - 不要编造知识库不存在的信息。
            
            
                    【知识库引用规则】
            
                    当回答内容来源于知识库时，必须添加引用标记。
            
                    引用格式：
                    [--cite:数字]
            
                    数字代表对应的知识库召回片段编号。
            
                    示例：
            
                    员工请假需要提前提交申请，并按照审批流程处理。[--cite:1]
            
                    转正流程需要经过相关负责人审核。[--cite:2]
            
            
                    引用要求：
            
                    1. 引用编号必须来自提供的知识库召回片段编号。
                    2. 不允许生成不存在的引用编号。
                    3. 如果一句话由多个知识库片段共同支持，可以添加多个引用：
            
                       示例：
                       员工请假需要提前申请，并完成审批流程。[--cite:1][--cite:3]
            
                    4. 只有来源于知识库的信息才添加引用。
                    5. 基于聊天历史回答时，不添加引用。
                    6. 不确定的信息不要添加引用。
                    7. 引用标记必须放在对应句子末尾。
                    8. 不要输出知识库原文，不要重复粘贴召回内容。
                    9. 不要解释引用标记含义，不要输出"引用来源"等额外说明。
            
            
                    【回答格式】
            
                    当依据知识库回答时：
            
                    1. 先给出简洁明确的结论。
                    2. 根据知识库内容进行归纳总结。
                    3. 在对应结论或事实后添加引用标记。
            
                    回答应保持专业、准确、简洁。
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

        //将document转化成DTO

        List<RetrievalSourceDTO> retrievalSourceDTOS = new ArrayList<>();

        for(Document doc : docs){
            RetrievalSourceDTO dto = RetrievalSourceDTO.of(doc);
            retrievalSourceDTOS.add(dto);
        }

        enqueueRetrievalSource(messageKey,retrievalSourceDTOS);

        StringBuilder ragText = new StringBuilder("以下内容为企业知识库召回结果：");

        for (Document doc : docs) {
            ragText.append(ragPromptService.buildAll(doc))
                    .append("\n\n------------------\n\n");
        }

        messages.add(
                new SystemMessage(ragText.toString())
        );

        // 历史消息
        messages.addAll(buildHistory(request.getConversationId()));

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
        messages.addAll(buildHistory(chatRequest.getConversationId()));
        String currentMessage = chatRequest.resolveMessage();
        if (currentMessage != null && !currentMessage.isBlank()) {
            messages.add(new UserMessage(currentMessage));
        }
        return messages;
    }

    private void ensureConversationReady(ChatRequest chatRequest) {
        if (chatRequest == null) {
            throw new IllegalArgumentException("需要chatRequest");
        }

        Long userId = SecurityUtils.getUserId();
        if (chatRequest.getConversationId() == null) {
            Long conversationId = conversationService.createConversation(userId);
            chatRequest.setConversationId(conversationId);
            return;
        }

        Conversation conversation = conversationMapper.selectByIdAndUserId(chatRequest.getConversationId(), userId);
        if (conversation == null) {
            throw new IllegalArgumentException("用户不属于该对话");
        }
        if (!ConversationStatus.ACTIVE.getValue().equals(conversation.getStatus())) {
            throw new IllegalArgumentException("对话不属于active状态");
        }
    }

    /**
     * 构建历史消息,不包含当前消息。
     * <p>不复用给前端传递历史消息，因为没有鉴权内容，因此只能用于内部拼接内容给AI</p>
     * <p>可能导致其他对话被泄露</p>
     * <p>对外接口使用ConversationSerivce下的</p>
     * @return
     */
    public List<Message> buildHistory(Long ConversationId){
        List<Message> history = new ArrayList<>();
        List<ConversationMessage> messagesList = conversationMessageMapper.selectByConversationId(ConversationId);
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
