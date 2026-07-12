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

    //向量召回
    public List<Document> retrieve(
            String query,
            Integer topK
    ) {


        List<Document> docs =
                vectorStore.similaritySearch(
                        SearchRequest.builder()
                                .query(query)
                                .topK(topK)
                                .similarityThreshold(0.35)
                                .build()
                );
        return docs;
    }

}