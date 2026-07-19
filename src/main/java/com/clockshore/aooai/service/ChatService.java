package com.clockshore.aooai.service;


import cn.hutool.core.util.IdUtil;
import com.clockshore.aooai.dto.message.ChatRequest;
import com.clockshore.aooai.dto.mq.RetrievalSourceDTO;
import com.clockshore.aooai.entity.Conversation;
import com.clockshore.aooai.entity.ConversationMessage;
import com.clockshore.aooai.entity.QueryRewriteResult;
import com.clockshore.aooai.enumType.CharacterRole;
import com.clockshore.aooai.enumType.ConversationStatus;
import com.clockshore.aooai.enumType.Prompt;
import com.clockshore.aooai.factory.ChatMessageEventFactory;
import com.clockshore.aooai.mapper.ConversationMapper;
import com.clockshore.aooai.mapper.ConversationMessageMapper;
import com.clockshore.aooai.mq.ChatMessageMqProducer;
import com.clockshore.aooai.mq.RetrievalSourceEvent;
import com.clockshore.aooai.mq.RetrievalSourceMqProducer;
import com.clockshore.aooai.security.SecurityUtils;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;


@Service
@Slf4j
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
    private QueryRewriteService queryRewriteService;

    @Resource
    private RetrievalSourceMqProducer retrievalSourceMqProducer;

    @Resource
    private ConversationService conversationService;

    @Resource
    private RerankService rerankService;

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
                new SystemMessage(Prompt.SYSTEM_RAG_ASSISTANT.getPrompt()));

        //解析当前问题
        String question = request.resolveMessage();

        //问题改写
        List<String> queries = queryRewriteService.rewrite(question).getQueries();

        //检索知识库,多路召回
        List<List<Document>> docs = new ArrayList<>();
        for(String query : queries){
            List<Document> temp = new ArrayList<>();
            try {
                List<Document> documents = knowledgeRetrievalService.retrieve(query, 5, request.getKnowledgeBaseId());
                int index = 1;
                for(Document document : documents){
                    document.getMetadata().put("querySource", query);
                    document.getMetadata().put("index", index++);
                    }
                temp.addAll(documents);
            } catch (RuntimeException e) {
                log.warn("召回失败, query={}", query, e);
            }
            docs.add(temp);
        }

        //召回去重
        docs = deduplicateRetrievedDocs(docs);


        //rerank
        rerankService.rerank()


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
         **/
        return null;
    }

    private List<List<Document>> deduplicateRetrievedDocs(List<List<Document>> docs){
        Map<String,DocumentScore> documentScoreMap = new HashMap<>();

        for (List<Document> documentList : docs){
            for (Document document : documentList){
                String key = String.valueOf(
                        document.getMetadata().get("chunkId")
                );
                Double score = document.getScore();

                //是否存在该ChunkId
                DocumentScore old = documentScoreMap.get(key);
                if(old == null){
                    //构造Value类，放入hashmap
                    documentScoreMap.put(key,
                            DocumentScore
                                    .builder()
                                    .document(document)
                                    .score(score)
                                    .build()
                    );
                }else{
                    if(isHigherScore(score, old.getScore())){
                        documentScoreMap.put(key,
                                DocumentScore
                                        .builder()
                                        .document(document)
                                        .score(score)
                                        .build()
                        );
                    }
                }
            }
        }
        
        List<List<Document>> deduplicatedDocs = new ArrayList<>();
        for (List<Document> documentList : docs) {
            List<Document> deduplicatedDocumentList = new ArrayList<>();
            for (Document document : documentList) {
                String key = String.valueOf(
                        document.getMetadata().get("chunkId")
                );
                DocumentScore documentScore = documentScoreMap.get(key);
                if (documentScore != null
                        && isSameScore(document.getScore(), documentScore.getScore())) {
                    deduplicatedDocumentList.add(document);
                }
            }
            deduplicatedDocs.add(deduplicatedDocumentList);
        }

        return deduplicatedDocs;
    }

    //避免装箱npe
    private boolean isHigherScore(Double score, Double oldScore) {
        if (score == null) {
            return false;
        }
        if (oldScore == null) {
            return true;
        }
        return Double.compare(score, oldScore) > 0;
    }

    //避免装箱npe
    private boolean isSameScore(Double score, Double retainedScore) {
        if (score == null || retainedScore == null) {
            return Objects.equals(score, retainedScore);
        }
        return Double.compare(score, retainedScore) == 0;
    }

    /**
     * 用于deduplicateRetrievedDocs方法中map内容
     */
    @Data
    @AllArgsConstructor
    @Builder
    static class DocumentScore {

        Document document;

        Double score;
    }

    /**
     * 使用这个方法时，只针对单个List<Document>的列表内去重，List<List<Document>>的不要使用这个方法
     * 旧方法，已弃用
     * @param docs
     * @return
     */
    private List<Document> deduplicate(List<Document> docs) {
        if (docs == null || docs.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, Document> deduplicatedDocs = new LinkedHashMap<>();
        for (Document doc : docs) {
            String key = buildRetrievalDedupKey(doc);
            if (!deduplicatedDocs.containsKey(key)) {
                deduplicatedDocs.put(key, doc);
            }
        }

        int duplicateCount = docs.size() - deduplicatedDocs.size();
        if (duplicateCount > 0) {
            log.info("召回去重完成, 原始数量={}, 去重后数量={}, 重复数量={}",
                    docs.size(), deduplicatedDocs.size(), duplicateCount);
        }
        return new ArrayList<>(deduplicatedDocs.values());
    }

    /**
     * 优先级选择，如果有 chunkId：直接返回，后面的 chunkKey 和 text 不看。
     */
    private String buildRetrievalDedupKey(Document doc) {
        if (doc == null) {
            return "empty";
        }
        Map<String, Object> metadata = doc.getMetadata();
        Object chunkId = metadata.get("chunkId");
        if (chunkId != null) {
            return "chunkId:" + chunkId;
        }
        Object chunkKey = metadata.get("chunkKey");
        if (chunkKey != null && !chunkKey.toString().isBlank()) {
            return "chunkKey:" + chunkKey;
        }
        String text = doc.getText();
        return "text:" + (text == null ? "" : text.replaceAll("\\s+", " ").trim());
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
        if(userId == null){
            throw new IllegalStateException("登录状态失效，请重新登录");
        }

        if (chatRequest.getConversationId() == null) {
            Long conversationId = conversationService.createConversation(userId);
            chatRequest.setConversationId(conversationId);
            return;
        }

        Conversation conversation = conversationMapper.selectByIdAndUserId(chatRequest.getConversationId(), userId);
        if (conversation == null) {
            throw new IllegalArgumentException("会话不存在");
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
