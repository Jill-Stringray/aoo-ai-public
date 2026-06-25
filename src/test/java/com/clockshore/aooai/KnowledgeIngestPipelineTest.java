package com.clockshore.aooai;

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

    @Test
    void testUploadParseSplitAndIndexPipeline() throws Exception {

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

        KnowledgeService.IngestResult result =
                knowledgeService.ingestAndIndex(file);

        System.out.println(
                "Document ID：" +
                        result.documentId()
        );

        System.out.println(
                "保存 Chunk 数量：" +
                        result.savedChunkCount()
        );

        System.out.println(
                "写入 VectorStore Chunk 数量：" +
                        result.indexedChunkCount()
        );

        assertThat(result.documentId()).isNotNull();
        assertThat(result.savedChunkCount()).isPositive();
        assertThat(result.indexedChunkCount())
                .isEqualTo(result.savedChunkCount());
    }
}
