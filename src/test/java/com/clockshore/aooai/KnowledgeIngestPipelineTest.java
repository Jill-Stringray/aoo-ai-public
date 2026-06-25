package com.clockshore.aooai;

import com.clockshore.aooai.mq.ChatMessageMqListener;
import com.clockshore.aooai.service.KnowledgeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class KnowledgeIngestPipelineTest {

    @Autowired
    private KnowledgeService knowledgeService;

    @Autowired
    private ChatMessageMqListener chatMessageMqListener;

    @Test
    void testUploadParseSplitAndIndexPipelineByMq() throws Exception {

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "demo-knowledge.pdf",
                        "application/pdf",
                        Files.readAllBytes(
                                Paths.get(
                                        "src/main/resources/demo-knowledge.pdf"
                                )
                        )
                );

        KnowledgeService.IngestResult preparedResult =
                knowledgeService.ingestAndSaveChunks(file);

        int indexedChunkCount =
                chatMessageMqListener.consumeEmbeddingTask(
                        preparedResult.documentId()
                );

        System.out.println(
                "Document ID：" +
                        preparedResult.documentId()
        );

        System.out.println(
                "保存 Chunk 数量：" +
                        preparedResult.savedChunkCount()
        );

        System.out.println(
                "MQ 写入 VectorStore Chunk 数量：" +
                        indexedChunkCount
        );

        assertThat(preparedResult.documentId()).isNotNull();
        assertThat(preparedResult.savedChunkCount()).isPositive();
        assertThat(indexedChunkCount)
                .isEqualTo(preparedResult.savedChunkCount());
    }
}
