package com.clockshore.aooai;

import com.clockshore.aooai.entity.LocalMqMessage;
import com.clockshore.aooai.enumType.LocalMqMessageStatus;
import com.clockshore.aooai.mq.ChatMessageMqListener;
import com.clockshore.aooai.mq.ChatMessageMqProducer;
import com.clockshore.aooai.mq.ChatMqConfig;
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
    private ChatMessageMqProducer chatMessageMqProducer;

    @Autowired
    private ChatMessageMqListener chatMessageMqListener;

    @Test
    void testUploadParseSplitPublishAndConsumePipelineByMq() throws Exception {

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

        LocalMqMessage embeddingTaskMessage =
                chatMessageMqProducer.publishEmbeddingTask(
                        preparedResult.documentId()
                );

        int indexedChunkCount =
                chatMessageMqListener.consumeEmbeddingPayload(
                        embeddingTaskMessage.getPayload()
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
                "本地 MQ 消息 ID：" +
                        embeddingTaskMessage.getId()
        );

        System.out.println(
                "MQ 写入 VectorStore Chunk 数量：" +
                        indexedChunkCount
        );

        assertThat(preparedResult.documentId()).isNotNull();
        assertThat(preparedResult.savedChunkCount()).isPositive();
        assertThat(embeddingTaskMessage.getId()).isNotNull();
        assertThat(embeddingTaskMessage.getTopic())
                .isEqualTo(ChatMqConfig.EMBEDDING_EXCHANGE);
        assertThat(embeddingTaskMessage.getRoutingKey())
                .isEqualTo(ChatMqConfig.EMBEDDING_ROUTING_KEY);
        assertThat(embeddingTaskMessage.getStatus())
                .isEqualTo(LocalMqMessageStatus.NEW.getValue());
        assertThat(indexedChunkCount)
                .isEqualTo(preparedResult.savedChunkCount());
    }
}
