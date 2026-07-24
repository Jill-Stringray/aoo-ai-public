package com.clockshore.aooai.service;


import cn.hutool.core.util.IdUtil;
import com.clockshore.aooai.config.AiMockProperties;
import com.clockshore.aooai.dto.message.ChatRequest;
import com.clockshore.aooai.dto.mq.RetrievalSourceDTO;
import com.clockshore.aooai.dto.rerank.RerankResult;
import com.clockshore.aooai.entity.Conversation;
import com.clockshore.aooai.entity.ConversationMessage;
import com.clockshore.aooai.entity.QueryRewriteResult;
import com.clockshore.aooai.enumType.CharacterRole;
import com.clockshore.aooai.enumType.ConversationStatus;
import com.clockshore.aooai.enumType.Prompt;
import com.clockshore.aooai.factory.ChatMessageEventFactory;
import com.clockshore.aooai.mapper.ConversationMapper;
import com.clockshore.aooai.mapper.ConversationMessageMapper;
import com.clockshore.aooai.mapper.MessageRetrievalSourceMapper;
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
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;


@Service
@Slf4j
public class ChatService {

    private static final double RERANK_SCORE_THRESHOLD = 0.44;
    private static final double ORIGINAL_SCORE_WEIGHT = 0.4;
    private static final double RERANK_SCORE_WEIGHT = 0.6;
    private static final int RRF_K = 60;
    private static final int MAX_HISTORY_CONTEXT_TOKENS = 32 * 1024;
    private static final int MESSAGE_TOKEN_OVERHEAD = 4;

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
    private MessageRetrievalSourceMapper messageRetrievalSourceMapper;

    @Resource
    private ConversationService conversationService;

    @Resource
    private RerankService rerankService;

    @Resource
    private SystemConfigService systemConfigService;

    @Resource
    private AiMockProperties aiMockProperties;

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

        if (aiMockProperties.isEnabled()) {
            return mockStreamChat(chatRequest, messageKey, null);
        }

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

    public Flux<ChatResponse> streamRegenerate(ChatRequest chatRequest, Long assistantMessageId) {
        ensureConversationReady(chatRequest);

        Long userId = SecurityUtils.getUserId();
        ConversationMessage oldAssistant = conversationMessageMapper.selectByIdAndUserId(
                chatRequest.getConversationId(),
                assistantMessageId,
                userId
        );
        if (oldAssistant == null) {
            throw new IllegalArgumentException("AI回复不存在或无权重新生成");
        }
        if (!CharacterRole.ASSISTANT.getValue().equals(oldAssistant.getRole())) {
            throw new IllegalArgumentException("只能重新生成AI回复");
        }

        ConversationMessage userMessage = conversationMessageMapper.selectPreviousUserMessage(
                oldAssistant.getConversationId(),
                oldAssistant.getId()
        );
        if (userMessage == null || userMessage.getContent() == null || userMessage.getContent().isBlank()) {
            throw new IllegalArgumentException("未找到可用于重新生成的问题");
        }

        chatRequest.setMessage(userMessage.getContent());
        String messageKey = IdUtil.getSnowflakeNextIdStr();

        if (aiMockProperties.isEnabled()) {
            return mockStreamChat(chatRequest, messageKey, userMessage.getId());
        }

        StringBuilder replyBuffer = new StringBuilder();
        AtomicReference<Usage> usageRef = new AtomicReference<>();
        long startedAt = System.currentTimeMillis();

        return chatClient.prompt()
                .messages(buildRagMessages(chatRequest, messageKey, userMessage.getId()))
                .stream()
                .chatResponse()
                .doOnNext(delta -> {
                    replyBuffer.append(delta.getResult().getOutput().getText());
                    if(delta.getMetadata() != null && delta.getMetadata().getUsage() != null){
                        usageRef.set(delta.getMetadata().getUsage());
                    }
                })
                .doOnComplete(() -> enqueueAssistantMessage(
                        chatRequest.getConversationId(),
                        userMessage.getId(),
                        messageKey,
                        replyBuffer.toString(),
                        chatRequest.getModel(),
                        usageRef.get(),
                        System.currentTimeMillis() - startedAt
                ));
    }

    private Flux<ChatResponse> mockStreamChat(ChatRequest chatRequest, String messageKey, Long parentMessageId) {
        List<String> mockTokens = List.of(
                "hello",
                "hello w",
                "hello wo",
                "hello wor",
                "hello world"
        );
        long startedAt = System.currentTimeMillis();

        return Flux.fromIterable(mockTokens)
                .delayElements(Duration.ofMillis(Math.max(0L, aiMockProperties.getDelay())))
                .map(this::mockChatResponse)
                .doOnComplete(() -> {
                    long latencyMs = System.currentTimeMillis() - startedAt;
                    String reply = mockTokens.get(mockTokens.size() - 1);
                    if (parentMessageId == null) {
                        enqueueAssistantMessage(
                                chatRequest.getConversationId(),
                                messageKey,
                                reply,
                                chatRequest.getModel(),
                                null,
                                latencyMs
                        );
                    } else {
                        enqueueAssistantMessage(
                                chatRequest.getConversationId(),
                                parentMessageId,
                                messageKey,
                                reply,
                                chatRequest.getModel(),
                                null,
                                latencyMs
                        );
                    }
                });
    }

    private ChatResponse mockChatResponse(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
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

    public void enqueueAssistantMessage(Long conversationId,
                                        Long parentMessageId,
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
                        parentMessageId,
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
        return buildRagMessages(request, messageKey, null);
    }

    public List<Message> buildRagMessages(ChatRequest request,String messageKey, Long historyBeforeId) {

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

        //召回去重，分高的优先保留
        List<Document> retrievedDocs = deduplicateRetrievedDocs(docs);

        //和原提问进行rerank，失败时保留原召回顺序，避免外部重排接口影响聊天主流程
        List<Document> rerankedDocs = rerankRetrievedDocs(question, retrievedDocs);

        //将document转化成DTO
        List<RetrievalSourceDTO> retrievalSourceDTOS = new ArrayList<>();

        for(Document doc : rerankedDocs){
            RetrievalSourceDTO dto = RetrievalSourceDTO.of(doc);
            retrievalSourceDTOS.add(dto);
        }

        enqueueRetrievalSource(messageKey,retrievalSourceDTOS);

        StringBuilder ragText = new StringBuilder("以下内容为企业知识库召回结果：");

        for (Document doc : rerankedDocs) {
            ragText.append(ragPromptService.buildAll(doc))
                    .append("\n\n------------------\n\n");
        }

        messages.add(
                new SystemMessage(ragText.toString())
        );

        // 历史消息
        messages.addAll(buildHistory(request.getConversationId(), historyBeforeId));

        // 加入当前用户问题
        messages.add(new UserMessage(question));

        return messages;
    }

    private List<Document> deduplicateRetrievedDocs(List<List<Document>> docs){
        Map<String, DocumentScore> documentScoreMap = new LinkedHashMap<>();

        for (List<Document> documentList : docs){
            if (documentList == null || documentList.isEmpty()) {
                continue;
            }
            for (int i = 0; i < documentList.size(); i++){
                Document document = documentList.get(i);
                if (document == null) {
                    continue;
                }
                String key = buildRetrievalDedupKey(document);
                double rrfScore = calculateRrfScore(i + 1);

                DocumentScore old = documentScoreMap.get(key);
                if(old == null){
                    documentScoreMap.put(key,
                            DocumentScore
                                    .builder()
                                    .document(document)
                                    .sourceScore(document.getScore())
                                    .score(rrfScore)
                                    .build()
                    );
                }else{
                    old.setScore(old.getScore() + rrfScore);
                    if(isHigherScore(document.getScore(), old.getSourceScore())){
                        old.setDocument(document);
                        old.setSourceScore(document.getScore());
                    }
                }
            }
        }

        return documentScoreMap.values()
                .stream()
                .sorted(Comparator.comparingDouble(DocumentScore::getScore).reversed())
                .map(item -> item.getDocument().mutate().score(item.getScore()).build())
                .toList();
    }

    private double calculateRrfScore(int rank) {
        return 1.0 / (RRF_K + rank);
    }

    private List<Document> rerankRetrievedDocs(String question, List<Document> retrievedDocs) {
        if (retrievedDocs == null || retrievedDocs.isEmpty()) {
            return List.of();
        }

        if (!Boolean.TRUE.equals(systemConfigService.getConfig().rerankEnabled())) {
            return retrievedDocs;
        }

        List<Document> rerankableDocs = retrievedDocs.stream()
                .filter(doc -> doc != null && doc.getText() != null && !doc.getText().isBlank())
                .toList();

        if (rerankableDocs.isEmpty()) {
            return retrievedDocs;
        }

        //最大返回五个
        try {
            List<RerankResult> rerankResults = rerankService.rerank(question, rerankableDocs);
            List<Document> rerankedDocs = rerankResults.stream()
                    .filter(this::isAboveRerankScoreThreshold)
                    .filter(result -> result.index() != null
                            && result.index() >= 0
                            && result.index() < rerankableDocs.size())
                    .map(result -> buildCombinedScoreDocument(result, rerankableDocs.get(result.index())))
                    .sorted(Comparator.comparingDouble(ScoredDocument::score).reversed())
                    .map(ScoredDocument::document)
                    .limit(5)
                    .toList();

            if (!rerankedDocs.isEmpty()) {
                return rerankedDocs;
            }
        } catch (RuntimeException e) {
            log.warn("Rerank失败，使用原召回顺序继续回答", e);
        }

        return retrievedDocs;
    }

    private ScoredDocument buildCombinedScoreDocument(RerankResult result, Document document) {
        double originalScore = document.getScore() == null ? 0.0 : document.getScore();
        double rerankScore = result.relevanceScore() == null ? 0.0 : result.relevanceScore();
        double combinedScore = originalScore * ORIGINAL_SCORE_WEIGHT + rerankScore * RERANK_SCORE_WEIGHT;
        return new ScoredDocument(document.mutate().score(combinedScore).build(), combinedScore);
    }

    private boolean isAboveRerankScoreThreshold(RerankResult result) {
        return result != null
                && result.relevanceScore() != null
                && Double.compare(result.relevanceScore(), RERANK_SCORE_THRESHOLD) >= 0;
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

        Double sourceScore;

        Double score;
    }

    private record ScoredDocument(Document document, double score) {
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
        return buildHistory(ConversationId, null);
    }

    public List<Message> buildHistory(Long ConversationId, Long beforeId){
        List<Message> history = new ArrayList<>();
        List<ConversationMessage> messagesList = conversationMessageMapper.selectByConversationId(ConversationId);
        int usedTokens = 0;
        int skippedCount = 0;

        for (int i = messagesList.size() - 1; i >= 0; i--) {
            ConversationMessage msg = messagesList.get(i);
            if (beforeId != null && msg.getId() >= beforeId) {
                continue;
            }
            Message message = toSpringAiMessage(msg);
            if (message == null) {
                continue;
            }

            int messageTokens = estimateMessageTokens(msg);
            if (!history.isEmpty() && usedTokens + messageTokens > MAX_HISTORY_CONTEXT_TOKENS) {
                skippedCount = i + 1;
                break;
            }

            history.add(0, message);
            usedTokens += messageTokens;
        }

        if (skippedCount > 0) {
            log.info("历史上下文超过限制，已跳过最早消息, conversationId={}, skippedCount={}, usedTokens={}, maxTokens={}",
                    ConversationId, skippedCount, usedTokens, MAX_HISTORY_CONTEXT_TOKENS);
        }
        return history;
    }

    private Message toSpringAiMessage(ConversationMessage msg) {
        if (msg == null) {
            return null;
        }
        String role = msg.getRole();
        if (CharacterRole.USER.getValue().equals(role)) {
            return new UserMessage(msg.getContent());
        } else if (CharacterRole.ASSISTANT.getValue().equals(role)) {
            return new AssistantMessage(msg.getContent());
        } else if (CharacterRole.SYSTEM.getValue().equals(role)) {
            return new SystemMessage(msg.getContent());
        }
        return null;
    }

    private int estimateMessageTokens(ConversationMessage msg) {
        if (msg == null || msg.getContent() == null || msg.getContent().isBlank()) {
            return MESSAGE_TOKEN_OVERHEAD;
        }
        return MESSAGE_TOKEN_OVERHEAD + estimateTextTokens(msg.getContent());
    }

    private int estimateTextTokens(String text) {
        int tokens = 0;
        int asciiRunLength = 0;

        for (int offset = 0; offset < text.length(); ) {
            int codePoint = text.codePointAt(offset);
            if (codePoint <= 0x7F) {
                asciiRunLength++;
            } else {
                tokens += estimateAsciiTokens(asciiRunLength);
                asciiRunLength = 0;
                tokens += isCjkCodePoint(codePoint) ? 1 : 2;
            }
            offset += Character.charCount(codePoint);
        }

        return tokens + estimateAsciiTokens(asciiRunLength);
    }

    private int estimateAsciiTokens(int charCount) {
        if (charCount <= 0) {
            return 0;
        }
        return Math.max(1, (charCount + 3) / 4);
    }

    private boolean isCjkCodePoint(int codePoint) {
        return (codePoint >= 0x3400 && codePoint <= 0x9FFF)
                || (codePoint >= 0xF900 && codePoint <= 0xFAFF)
                || (codePoint >= 0x20000 && codePoint <= 0x2FA1F);
    }




}
