package com.clockshore.aooai;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest(classes = AooAiApplication.class)
public class RetrievalIT {

    @Resource
    private VectorStore vectorStore;

    @Test
    void testSimilaritySearch() {

        System.out.println("\n\n******testSimilaritySearch********\n\n");

        List<Document> documents =
                vectorStore.similaritySearch(
                        SearchRequest.builder()
                                .query("Cyberpunk RED 与 VA-11 HALL-A 的关系是什么？")
                                .topK(5)
                                .build()
                );

        assert documents != null;

        System.out.println(
                "召回数量：" + documents.size()
        );

        for (Document doc : documents) {

            System.out.println(
                    "=================================="
            );

            System.out.println(
                    doc.getText()
            );

            System.out.println(
                    doc.getMetadata()
            );
        }

        System.out.println("\n\n******testSimilaritySearch********\n\n");
    }
}
