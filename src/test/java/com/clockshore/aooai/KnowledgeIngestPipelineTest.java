package com.clockshore.aooai;

import com.clockshore.aooai.Utility.ToJsonUtil;
import com.clockshore.aooai.entity.DocumentChunk;
import com.clockshore.aooai.entity.KnowledgeDocument;
import com.clockshore.aooai.mapper.DocumentChunkMapper;
import com.clockshore.aooai.mapper.KnowledgeDocumentMapper;
import com.clockshore.aooai.service.DocumentReaderService;
import com.clockshore.aooai.service.KnowledgeService;
import com.clockshore.aooai.service.TokenSplitterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeIngestPipelineTest {

    @Mock
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Mock
    private DocumentChunkMapper documentChunkMapper;

    @Mock
    private VectorStore vectorStore;

    @Test
    void ingestAndIndexShouldRunWholeRagPipeline() throws Exception {
        KnowledgeService knowledgeService = new KnowledgeService(vectorStore);
        ReflectionTestUtils.setField(knowledgeService, "knowledgeDocumentMapper", knowledgeDocumentMapper);
        ReflectionTestUtils.setField(knowledgeService, "documentChunkMapper", documentChunkMapper);
        ReflectionTestUtils.setField(knowledgeService, "documentReaderService", new DocumentReaderService());
        ReflectionTestUtils.setField(knowledgeService, "tokenSplitterService", new TokenSplitterService());

        ToJsonUtil toJsonUtil = new ToJsonUtil();
        ReflectionTestUtils.setField(toJsonUtil, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(knowledgeService, "toJsonUtil", toJsonUtil);

        doAnswer(invocation -> {
            KnowledgeDocument document = invocation.getArgument(0);
            document.setId(100L);
            return 1;
        }).when(knowledgeDocumentMapper).insert(any(KnowledgeDocument.class));

        AtomicReference<List<DocumentChunk>> savedChunks = new AtomicReference<>();
        doAnswer(invocation -> {
            List<DocumentChunk> chunks = new ArrayList<>(invocation.getArgument(0));
            for (int i = 0; i < chunks.size(); i++) {
                chunks.get(i).setId((long) i + 1);
            }
            savedChunks.set(chunks);
            return chunks.size();
        }).when(documentChunkMapper).batchInsert(any());
        when(documentChunkMapper.selectNewByDocumentId(100L)).thenAnswer(invocation -> savedChunks.get());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "rag-pipeline.txt",
                "text/plain",
                "RAG pipeline integration test content".getBytes(StandardCharsets.UTF_8)
        );

        KnowledgeService.IngestResult result = knowledgeService.ingestAndIndex(file);

        assertThat(result.documentId()).isEqualTo(100L);
        assertThat(result.savedChunkCount()).isPositive();
        assertThat(result.indexedChunkCount()).isEqualTo(result.savedChunkCount());

        verify(knowledgeDocumentMapper).updateStatus(100L, "PARSING");
        verify(knowledgeDocumentMapper).updateStatus(100L, "CHUNKING");
        verify(knowledgeDocumentMapper).updateStatus(100L, "INDEXING");
        verify(knowledgeDocumentMapper).updateStatus(100L, "READY");
        verify(documentChunkMapper).updateStatusByDocumentId(100L, "EMBEDDED");

        ArgumentCaptor<List<Document>> vectorDocsCaptor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(vectorDocsCaptor.capture());
        assertThat(vectorDocsCaptor.getValue())
                .hasSize(result.savedChunkCount())
                .allSatisfy(document -> {
                    assertThat(document.getText()).isNotBlank();
                    assertThat(document.getMetadata()).containsEntry("documentId", 100L);
                    assertThat(document.getMetadata()).containsKey("chunkKey");
                });
    }
}
