package com.clockshore.aooai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;


import java.util.List;

@Service
@RequiredArgsConstructor
public class KnowledgeRetrievalService {

    private final VectorStore vectorStore;
    private final SystemConfigService systemConfigService;

    //向量召回
    public List<Document> retrieve(
            String query,
            Integer topK
    ) {
        return retrieve(query, topK, null);
    }

    public List<Document> retrieve(
            String query,
            Integer topK,
            Long knowledgeBaseId
    ) {
        SystemConfigService.SystemConfig config = systemConfigService.getConfig();
        SearchRequest.Builder builder = SearchRequest.builder()
                .query(query)
                .topK(topK == null ? config.topK() : topK)
                .similarityThreshold(config.similarityThreshold());

        if (knowledgeBaseId != null) {
            builder.filterExpression("knowledgeBaseId == " + knowledgeBaseId);
        }

        return vectorStore.similaritySearch(builder.build());
    }

}
