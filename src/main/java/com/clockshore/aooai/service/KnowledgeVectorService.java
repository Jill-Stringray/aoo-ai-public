package com.clockshore.aooai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KnowledgeVectorService {

    private final VectorStore vectorStore;

    public void deleteByDocumentId(Long documentId) {
        if (documentId == null) {
            return;
        }
        vectorStore.delete(eq("documentId", documentId));
    }

    public void deleteByKnowledgeBaseId(Long knowledgeBaseId) {
        if (knowledgeBaseId == null) {
            return;
        }
        vectorStore.delete(eq("knowledgeBaseId", knowledgeBaseId));
    }

    private Filter.Expression eq(String key, Long value) {
        return new FilterExpressionBuilder()
                .eq(key, value)
                .build();
    }
}
