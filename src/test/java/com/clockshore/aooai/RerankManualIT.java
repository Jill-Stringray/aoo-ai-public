package com.clockshore.aooai;

import com.clockshore.aooai.dto.rerank.RerankResult;
import com.clockshore.aooai.service.RerankService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

@SpringBootTest(classes = AooAiApplication.class)
class RerankManualIT {

    @Resource
    private RerankService rerankService;

    @Test
    @DisplayName("Manual Rerank")
    void rerankManualDocuments() {
        rerankManual(
                "吉尔是谁",
                "Jill Stingray 是一名调酒师，也是 VA-11 Hall-A 中的主角。",
                "天气晴朗时适合出门散步。",
                "吉尔在故事里负责和客人聊天，并通过调酒推进剧情。",
                "数据库索引可以提高查询效率。"
        );
    }

    private void rerankManual(String query, String... documentTexts) {
        List<Document> documents = Arrays.stream(documentTexts)
                .map(Document::new)
                .toList();

        List<RerankResult> results = rerankService.rerank(query, documents);

        System.out.println("\n\n******manual rerank********\n");
        System.out.println("query: " + query);
        System.out.println("\nresults:");
        for (RerankResult result : results) {
            System.out.printf(
                    "index=%d, relevance_score=%.6f, text=%s%n",
                    result.index(),
                    result.relevanceScore(),
                    result.document() == null ? null : result.document().text()
            );
        }
        System.out.println("\n******manual rerank********\n\n");
    }
}
