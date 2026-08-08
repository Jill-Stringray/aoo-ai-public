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
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;


@Service
@Slf4j
public class ChatService {

    private static final double RERANK_SCORE_THRESHOLD = 0.44;
    private static final double ORIGINAL_SCORE_WEIGHT = 0.4;
    private static final double RERANK_SCORE_WEIGHT = 0.6;
    private static final int RRF_K = 60;
    private static final int RRF_TOP_N = 10;
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
                       ConversationMessageMapper conversationMessageMapper,
                       ObjectProvider<ToolCallbackProvider> toolCallbackProviders
    ){
        ToolCallbackProvider[] providers = toolCallbackProviders.orderedStream()
                .toArray(ToolCallbackProvider[]::new);
        if (providers.length > 0) {
            builder.defaultToolCallbacks(providers);
            log.info("registered tool callback providers for ChatClient, count={}", providers.length);
            logMcpTools(providers);
        } else {
            log.warn("no tool callback providers found for ChatClient");
        }
        this.chatClient = builder.build();
        this.conversationMessageMapper = conversationMessageMapper;
    }

    private void logMcpTools(ToolCallbackProvider[] providers) {
        for (ToolCallbackProvider provider : providers) {
            try {
                ToolCallback[] toolCallbacks = provider.getToolCallbacks();
                List<String> toolNames = new ArrayList<>();
                for (ToolCallback toolCallback : toolCallbacks) {
                    toolNames.add(toolCallback.getToolDefinition().name());
                }
                log.info("loaded MCP tools from provider={}, count={}, tools={}",
                        provider.getClass().getSimpleName(),
                        toolCallbacks.length,
                        toolNames);
            } catch (RuntimeException e) {
                log.warn("failed to load MCP tools from provider={}", provider.getClass().getSimpleName(), e);
            }
        }
    }

    public String nonStreamChat(ChatRequest chatRequest) {

        //Advisor = ChatClient 的“拦截器 / 增强器 / 插件系统”
        ChatCostStat costStat = new ChatCostStat(System.nanoTime());
        ensureConversationReady(chatRequest);
        long startedAt = System.currentTimeMillis();
        //生成雪花key
        String messageKey = IdUtil.getSnowflakeNextIdStr();

        List<Message> messages = buildRagMessages(chatRequest, messageKey, costStat);
        long aiInvokeStartedAt = System.nanoTime();
        ChatResponse response = chatClient.prompt()
                .messages(messages)
                .call()
                .chatResponse();
        costStat.setAiInvokeCostMs(elapsedMs(aiInvokeStartedAt));

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
        logChatCost(costStat);
        return reply;
    }



    public Flux<ChatResponse> streamChat(ChatRequest chatRequest){


        ChatCostStat costStat = new ChatCostStat(System.nanoTime());
        ensureConversationReady(chatRequest);


        // 1. 保存用户消息
        enqueueUserMessage(chatRequest.getConversationId(),chatRequest.getMessage());

        //生成雪花
        String messageKey = IdUtil.getSnowflakeNextIdStr();

        StringBuilder replyBuffer = new StringBuilder();
        AtomicReference<Usage> usageRef = new AtomicReference<>();
        long startedAt = System.currentTimeMillis();
        List<Message> messages = buildRagMessages(chatRequest, messageKey, costStat);

        if (aiMockProperties.isEnabled()) {
            log.info("ai mock enabled, skip real ai invoke");
            return mockStreamChat(chatRequest, messageKey, null, costStat);
        }

        AtomicLong aiStartedAt = new AtomicLong();
        AtomicBoolean firstTokenReceived = new AtomicBoolean(false);

        return chatClient.prompt()
                .messages(messages)
                .stream()
                .chatResponse()
                .doOnSubscribe(subscription -> aiStartedAt.set(System.nanoTime()))
                .doOnNext(delta -> {
                    long currentAiStartedAt = aiStartedAt.get();
                    if (currentAiStartedAt > 0 && firstTokenReceived.compareAndSet(false, true)) {
                        log.info("ai first token cost={}ms", elapsedMs(currentAiStartedAt));
                    }
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
                ))
                .doOnComplete(() -> {
                    long currentAiStartedAt = aiStartedAt.get();
                    if (currentAiStartedAt > 0) {
                        costStat.setAiInvokeCostMs(elapsedMs(currentAiStartedAt));
                        log.info("ai total cost={}ms", costStat.getAiInvokeCostMs());
                    }
                    logChatCost(costStat);
                });
    }

    public Flux<ChatResponse> streamRegenerate(ChatRequest chatRequest, Long assistantMessageId) {
        ChatCostStat costStat = new ChatCostStat(System.nanoTime());
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

        StringBuilder replyBuffer = new StringBuilder();
        AtomicReference<Usage> usageRef = new AtomicReference<>();
        long startedAt = System.currentTimeMillis();
        List<Message> messages = buildRagMessages(chatRequest, messageKey, userMessage.getId(), costStat);

        if (aiMockProperties.isEnabled()) {
            log.info("ai mock enabled, skip real ai invoke");
            return mockStreamChat(chatRequest, messageKey, userMessage.getId(), costStat);
        }

        AtomicLong aiStartedAt = new AtomicLong();
        AtomicBoolean firstTokenReceived = new AtomicBoolean(false);

        return chatClient.prompt()
                .messages(messages)
                .stream()
                .chatResponse()
                .doOnSubscribe(subscription -> aiStartedAt.set(System.nanoTime()))
                .doOnNext(delta -> {
                    long currentAiStartedAt = aiStartedAt.get();
                    if (currentAiStartedAt > 0 && firstTokenReceived.compareAndSet(false, true)) {
                        log.info("ai first token cost={}ms", elapsedMs(currentAiStartedAt));
                    }
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
                ))
                .doOnComplete(() -> {
                    long currentAiStartedAt = aiStartedAt.get();
                    if (currentAiStartedAt > 0) {
                        costStat.setAiInvokeCostMs(elapsedMs(currentAiStartedAt));
                        log.info("ai total cost={}ms", costStat.getAiInvokeCostMs());
                    }
                    logChatCost(costStat);
                });
    }

    private Flux<ChatResponse> mockStreamChat(ChatRequest chatRequest, String messageKey, Long parentMessageId) {
        return mockStreamChat(chatRequest, messageKey, parentMessageId, new ChatCostStat(System.nanoTime()));
    }

    private Flux<ChatResponse> mockStreamChat(ChatRequest chatRequest, String messageKey, Long parentMessageId, ChatCostStat costStat) {
        List<String> mockTokens = List.of(
                "hello",
                "hello w",
                "hello wo",
                "hello wor",
                "hello world"
        );
        long startedAt = System.currentTimeMillis();
        AtomicLong aiStartedAt = new AtomicLong();
        AtomicBoolean firstTokenReceived = new AtomicBoolean(false);

        return Flux.fromIterable(mockTokens)
                .delayElements(Duration.ofMillis(Math.max(0L, aiMockProperties.getDelay())))
                .map(this::mockChatResponse)
                .doOnSubscribe(subscription -> aiStartedAt.set(System.nanoTime()))
                .doOnNext(delta -> {
                    long currentAiStartedAt = aiStartedAt.get();
                    if (currentAiStartedAt > 0 && firstTokenReceived.compareAndSet(false, true)) {
                        log.info("ai first token cost={}ms", elapsedMs(currentAiStartedAt));
                    }
                })
                .doOnComplete(() -> {
                    long latencyMs = System.currentTimeMillis() - startedAt;
                    long currentAiStartedAt = aiStartedAt.get();
                    if (currentAiStartedAt > 0) {
                        costStat.setAiInvokeCostMs(elapsedMs(currentAiStartedAt));
                        log.info("ai total cost={}ms", costStat.getAiInvokeCostMs());
                    }
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
                    logChatCost(costStat);
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
        return buildRagMessages(request, messageKey, (Long) null);
    }

    public List<Message> buildRagMessages(ChatRequest request,String messageKey, Long historyBeforeId) {
        return buildRagMessages(request, messageKey, historyBeforeId, null);
    }

    private List<Message> buildRagMessages(ChatRequest request, String messageKey, ChatCostStat costStat) {
        return buildRagMessages(request, messageKey, null, costStat);
    }

    private List<Message> buildRagMessages(ChatRequest request,String messageKey, Long historyBeforeId, ChatCostStat costStat) {

        long promptBuildStartedAt = System.nanoTime();

        List<Message> messages = new ArrayList<>();

        //基础提示词
        messages.add(
                new SystemMessage(Prompt.SYSTEM_RAG_ASSISTANT.getPrompt()));

        //解析当前问题
        String question = request.resolveMessage();

        //问题改写
        long queryRewriteStartedAt = System.nanoTime();
        List<String> queries = queryRewriteService.rewrite(question).getQueries();
        if (costStat != null) {
            costStat.setQueryRewriteCostMs(elapsedMs(queryRewriteStartedAt));
        }

        //检索知识库,多路召回
        List<List<Document>> docs = new ArrayList<>();
        for(String query : queries){
            List<Document> temp = new ArrayList<>();
            long retrieveStartedAt = System.nanoTime();
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
            } finally {
                addRetrieveCost(costStat, elapsedMs(retrieveStartedAt));
            }
            docs.add(temp);
        }

        //召回去重，分高的优先保留
        long deduplicateStartedAt = System.nanoTime();
        List<Document> retrievedDocs = deduplicateRetrievedDocs(docs);
        if (costStat != null) {
            costStat.setDeduplicateCostMs(elapsedMs(deduplicateStartedAt));
        }

        //和原提问进行rerank，失败时保留原召回顺序，避免外部重排接口影响聊天主流程
        long rerankStartedAt = System.nanoTime();
        List<Document> rerankedDocs = rerankRetrievedDocs(question, retrievedDocs);
        if (costStat != null && Boolean.TRUE.equals(systemConfigService.getConfig().rerankEnabled())) {
            costStat.setRerankCostMs(elapsedMs(rerankStartedAt));
        }

        //将document转化成DTO
        List<RetrievalSourceDTO> retrievalSourceDTOS = new ArrayList<>();

        long retrievalSourceBuildStartedAt = System.nanoTime();
        for(Document doc : rerankedDocs){
            RetrievalSourceDTO dto = RetrievalSourceDTO.of(doc);
            retrievalSourceDTOS.add(dto);
        }

        enqueueRetrievalSource(messageKey,retrievalSourceDTOS);
        if (costStat != null) {
            costStat.setRetrievalSourceBuildCostMs(elapsedMs(retrievalSourceBuildStartedAt));
        }

        long ragPromptBuildStartedAt = System.nanoTime();
        StringBuilder ragText = new StringBuilder("以下内容为企业知识库召回结果：");

        for (Document doc : rerankedDocs) {
            ragText.append(ragPromptService.buildAll(doc))
                    .append("\n\n------------------\n\n");
        }
        if (costStat != null) {
            costStat.setRagPromptBuildCostMs(elapsedMs(ragPromptBuildStartedAt));
        }

        messages.add(
                new SystemMessage(ragText.toString())
        );

        // 历史消息
        long historyBuildStartedAt = System.nanoTime();
        messages.addAll(buildHistory(request.getConversationId(), historyBeforeId));
        if (costStat != null) {
            costStat.setHistoryBuildCostMs(elapsedMs(historyBuildStartedAt));
        }

        // 加入当前用户问题
        messages.add(new UserMessage(question));

        if (costStat != null) {
            long promptBuildCostMs = elapsedMs(promptBuildStartedAt)
                    - costStat.getRetrieveCostMs()
                    - costStat.getRerankCostMs();
            costStat.setPromptBuildCostMs(Math.max(0L, promptBuildCostMs));
            long knownPromptBuildCostMs = costStat.getQueryRewriteCostMs()
                    + costStat.getDeduplicateCostMs()
                    + costStat.getRetrievalSourceBuildCostMs()
                    + costStat.getRagPromptBuildCostMs()
                    + costStat.getHistoryBuildCostMs();
            costStat.setPromptBuildOtherCostMs(Math.max(0L, costStat.getPromptBuildCostMs() - knownPromptBuildCostMs));
        }

        return messages;
    }

    private void addRetrieveCost(ChatCostStat costStat, long costMs) {
        if (costStat != null) {
            costStat.setRetrieveCostMs(costStat.getRetrieveCostMs() + costMs);
        }
    }

    private void logChatCost(ChatCostStat costStat) {
        if (costStat == null) {
            return;
        }
        log.info("service total cost={}ms", elapsedMs(costStat.getServiceStartedAt()));
        log.info("retrieve cost={}ms", costStat.getRetrieveCostMs());
        log.info("rerank cost={}ms", costStat.getRerankCostMs());
        log.info("prompt build cost={}ms", costStat.getPromptBuildCostMs());
        log.info("prompt build detail: queryRewrite={}ms, deduplicate={}ms, retrievalSource={}ms, ragPrompt={}ms, history={}ms, other={}ms",
                costStat.getQueryRewriteCostMs(),
                costStat.getDeduplicateCostMs(),
                costStat.getRetrievalSourceBuildCostMs(),
                costStat.getRagPromptBuildCostMs(),
                costStat.getHistoryBuildCostMs(),
                costStat.getPromptBuildOtherCostMs());
        log.info("ai invoke cost={}ms", costStat.getAiInvokeCostMs());
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
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
                .limit(RRF_TOP_N)
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

    @Data
    static class ChatCostStat {

        ChatCostStat(long serviceStartedAt) {
            this.serviceStartedAt = serviceStartedAt;
        }

        private long serviceStartedAt;

        private long retrieveCostMs;

        private long rerankCostMs;

        private long promptBuildCostMs;

        private long queryRewriteCostMs;

        private long deduplicateCostMs;

        private long retrievalSourceBuildCostMs;

        private long ragPromptBuildCostMs;

        private long historyBuildCostMs;

        private long promptBuildOtherCostMs;

        private long aiInvokeCostMs;
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
