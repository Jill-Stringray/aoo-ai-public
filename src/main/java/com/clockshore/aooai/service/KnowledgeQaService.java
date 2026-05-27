package com.clockshore.aooai.service;

import com.clockshore.aooai.dto.AskRequest;
import com.clockshore.aooai.dto.AskResponse;
import com.clockshore.aooai.dto.SourceRef;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;

@Service
public class KnowledgeQaService {
    public AskResponse ask(AskRequest request) {
        return AskResponse.builder()
                .answer("这是企业级知识库问答平台的占位回答，后续将接入完整 RAG 流程。")
                .sources(List.of(SourceRef.builder()
                        .documentName("系统设计文档.md")
                        .pageNumber(1)
                        .chunkId("chunk-001")
                        .build()))
                .promptTokens(120)
                .completionTokens(80)
                .totalTokens(200)
                .build();
    }

    public Flux<String> askStream(AskRequest request) {
        return Flux.just("正在进行 Query Rewrite...", "正在执行 Hybrid Search...", "正在 Rerank 与上下文压缩...", "已生成回答（占位）")
                .delayElements(Duration.ofMillis(300));
    }
}
