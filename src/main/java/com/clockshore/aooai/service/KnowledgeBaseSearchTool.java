package com.clockshore.aooai.service;

import com.clockshore.aooai.dto.mq.RetrievalSourceDTO;
import com.clockshore.aooai.dto.rerank.RerankResult;
import com.clockshore.aooai.mq.RetrievalSourceEvent;
import com.clockshore.aooai.mq.RetrievalSourceMqProducer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeBaseSearchTool {

    public static final String TOOL_CONTEXT_MESSAGE_KEY = "messageKey";
    public static final String TOOL_CONTEXT_KNOWLEDGE_BASE_ID = "knowledgeBaseId";

    private static final double RERANK_SCORE_THRESHOLD = 0.44;
    private static final double ORIGINAL_SCORE_WEIGHT = 0.4;
    private static final double RERANK_SCORE_WEIGHT = 0.6;
    private static final int RRF_K = 60;
    private static final int RRF_TOP_N = 10;
    private static final int DEFAULT_TOP_K = 5;
    private static final int MAX_TOP_K = 10;

    private final KnowledgeRetrievalService knowledgeRetrievalService;
    private final QueryRewriteService queryRewriteService;
    private final RerankService rerankService;
    private final SystemConfigService systemConfigService;
    private final RetrievalSourceMqProducer retrievalSourceMqProducer;

    @Tool(
            name = "search_knowledge_base",
            description = """
                    查询企业本地知识库。仅当用户问题涉及企业业务、制度、流程、产品、内部文档、
                    或需要以知识库事实为依据时调用。返回带编号的知识库片段，回答引用时必须使用 [--cite:编号]。
                    """
    )
    public String searchKnowledgeBase(
            @ToolParam(description = "用户问题或适合知识库检索的查询语句。") String query,
            @ToolParam(description = "最多返回的片段数量，默认5，最大10。", required = false) Integer topK,
            ToolContext toolContext
    ) {
        String normalizedQuery = query == null ? "" : query.trim();
        if (normalizedQuery.isBlank()) {
            return "知识库查询失败：query不能为空。";
        }

        int resolvedTopK = resolveTopK(topK);
        Long knowledgeBaseId = resolveKnowledgeBaseId(toolContext);
        String messageKey = resolveMessageKey(toolContext);

        List<Document> docs = retrieve(normalizedQuery, resolvedTopK, knowledgeBaseId);
        if (docs.isEmpty()) {
            return "知识库中未找到相关内容。";
        }

        enqueueRetrievalSource(messageKey, docs);
        return buildToolResult(docs);
    }

    private List<Document> retrieve(String query, int topK, Long knowledgeBaseId) {
        List<String> queries = rewriteQueries(query);
        List<List<Document>> docs = new ArrayList<>();

        for (String rewrittenQuery : queries) {
            try {
                List<Document> documents = knowledgeRetrievalService.retrieve(rewrittenQuery, topK, knowledgeBaseId);
                int index = 1;
                for (Document document : documents) {
                    document.getMetadata().put("querySource", rewrittenQuery);
                    document.getMetadata().put("index", index++);
                }
                docs.add(documents);
            } catch (RuntimeException e) {
                log.warn("知识库工具召回失败, query={}", rewrittenQuery, e);
                docs.add(List.of());
            }
        }

        List<Document> retrievedDocs = deduplicateRetrievedDocs(docs);
        return rerankRetrievedDocs(query, retrievedDocs);
    }

    private List<String> rewriteQueries(String query) {
        try {
            List<String> queries = queryRewriteService.rewrite(query).getQueries();
            if (queries != null && !queries.isEmpty()) {
                return queries;
            }
        } catch (RuntimeException e) {
            log.warn("知识库工具问题改写失败，使用原始问题检索", e);
        }
        return List.of(query);
    }

    private void enqueueRetrievalSource(String messageKey, List<Document> docs) {
        if (messageKey == null || messageKey.isBlank() || docs == null || docs.isEmpty()) {
            return;
        }

        List<RetrievalSourceDTO> sources = docs.stream()
                .map(RetrievalSourceDTO::of)
                .toList();

        retrievalSourceMqProducer.publishRetrievalSourcePersist(
                RetrievalSourceEvent.builder()
                        .messageKey(messageKey)
                        .sources(sources)
                        .build()
        );
    }

    private String buildToolResult(List<Document> docs) {
        StringBuilder result = new StringBuilder("以下是知识库召回片段，请仅使用这些片段回答需要知识库依据的问题：\n\n");

        int index = 1;
        for (Document doc : docs) {
            Map<String, Object> metadata = doc.getMetadata();
            result.append("【片段 ").append(index).append("】\n")
                    .append("引用标记：[--cite:").append(index).append("]\n")
                    .append("来源：").append(valueOrDefault(metadata.get("fileName"), "未知文档"))
                    .append(" 第").append(valueOrDefault(firstPresent(metadata, "pageNo", "page_number"), "未知")).append("页\n")
                    .append("内容：\n")
                    .append(doc.getText() == null ? "" : doc.getText().trim())
                    .append("\n\n");
            index++;
        }

        return result.toString();
    }

    private int resolveTopK(Integer topK) {
        if (topK == null || topK <= 0) {
            return DEFAULT_TOP_K;
        }
        return Math.min(topK, MAX_TOP_K);
    }

    private Long resolveKnowledgeBaseId(ToolContext toolContext) {
        if (toolContext == null || toolContext.getContext() == null) {
            return null;
        }
        return toLong(toolContext.getContext().get(TOOL_CONTEXT_KNOWLEDGE_BASE_ID));
    }

    private String resolveMessageKey(ToolContext toolContext) {
        if (toolContext == null || toolContext.getContext() == null) {
            return null;
        }
        Object value = toolContext.getContext().get(TOOL_CONTEXT_MESSAGE_KEY);
        return value == null ? null : value.toString();
    }

    private Object firstPresent(Map<String, Object> metadata, String... keys) {
        for (String key : keys) {
            Object value = metadata.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String valueOrDefault(Object value, String defaultValue) {
        return value == null ? defaultValue : value.toString();
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = value.toString();
        return text.isBlank() ? null : Long.valueOf(text);
    }

    private List<Document> deduplicateRetrievedDocs(List<List<Document>> docs) {
        Map<String, DocumentScore> documentScoreMap = new LinkedHashMap<>();

        for (List<Document> documentList : docs) {
            if (documentList == null || documentList.isEmpty()) {
                continue;
            }
            for (int i = 0; i < documentList.size(); i++) {
                Document document = documentList.get(i);
                if (document == null) {
                    continue;
                }
                String key = buildRetrievalDedupKey(document);
                double rrfScore = calculateRrfScore(i + 1);

                DocumentScore old = documentScoreMap.get(key);
                if (old == null) {
                    documentScoreMap.put(key,
                            DocumentScore.builder()
                                    .document(document)
                                    .sourceScore(document.getScore())
                                    .score(rrfScore)
                                    .build()
                    );
                } else {
                    old.setScore(old.getScore() + rrfScore);
                    if (isHigherScore(document.getScore(), old.getSourceScore())) {
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

    private String buildRetrievalDedupKey(Document document) {
        Map<String, Object> metadata = document.getMetadata();
        Object chunkId = metadata.get("chunkId");
        if (chunkId != null) {
            return "chunk:" + chunkId;
        }
        return "text:" + Objects.toString(document.getText(), "");
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
                    .limit(DEFAULT_TOP_K)
                    .toList();

            if (!rerankedDocs.isEmpty()) {
                return rerankedDocs;
            }
        } catch (RuntimeException e) {
            log.warn("知识库工具Rerank失败，使用原召回顺序继续回答", e);
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

    private boolean isHigherScore(Double score, Double oldScore) {
        if (score == null) {
            return false;
        }
        if (oldScore == null) {
            return true;
        }
        return Double.compare(score, oldScore) > 0;
    }

    private record ScoredDocument(Document document, double score) {
    }

    @Data
    @AllArgsConstructor
    @Builder
    static class DocumentScore {

        Document document;

        Double sourceScore;

        Double score;
    }
}
