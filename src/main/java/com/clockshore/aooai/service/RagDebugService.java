package com.clockshore.aooai.service;

import com.clockshore.aooai.dto.mq.RetrievalSourceDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RagDebugService {

    private final EmbeddingModel embeddingModel;
    private final KnowledgeRetrievalService knowledgeRetrievalService;
    private final RagPromptService ragPromptService;
    private final SystemConfigService systemConfigService;
    private final ChatClient.Builder chatClientBuilder;

    public RagDebugResult debug(RagDebugRequest request) {
        String question = normalizeQuestion(request == null ? null : request.question());
        SystemConfigService.SystemConfig config = systemConfigService.getConfig();
        Integer topK = request != null && request.topK() != null ? request.topK() : config.topK();
        Long knowledgeBaseId = request == null ? null : request.knowledgeBaseId();

        float[] embedding = embeddingModel.embed(question);
        List<Document> docs = knowledgeRetrievalService.retrieve(question, topK, knowledgeBaseId);
        List<RagDebugChunk> chunks = toChunks(docs);
        String context = buildContext(docs);

        ChatResponse response = chatClientBuilder.build()
                .prompt()
                .messages(buildMessages(context, question))
                .call()
                .chatResponse();

        String answer = response == null || response.getResult() == null
                ? ""
                : response.getResult().getOutput().getText();

        return new RagDebugResult(
                question,
                config.embeddingModel(),
                embedding.length,
                sampleEmbedding(embedding),
                topK,
                config.similarityThreshold(),
                chunks,
                context,
                answer
        );
    }

    private String normalizeQuestion(String question) {
        String trimmed = question == null ? "" : question.trim();
        if (!trimmed.isBlank()) {
            return trimmed;
        }
        return "企业知识库中有哪些内容可以用于回答用户问题？";
    }

    private List<RagDebugChunk> toChunks(List<Document> docs) {
        List<RagDebugChunk> chunks = new ArrayList<>();
        int index = 1;
        for (Document doc : docs) {
            RetrievalSourceDTO source = RetrievalSourceDTO.of(doc);
            chunks.add(new RagDebugChunk(
                    index++,
                    source.getDocumentId(),
                    source.getChunkId(),
                    source.getFileName(),
                    source.getKnowledgeBaseId(),
                    source.getPageNumber(),
                    source.getChunkIndex(),
                    source.getScore(),
                    source.getContent()
            ));
        }
        return chunks;
    }

    private String buildContext(List<Document> docs) {
        StringBuilder ragText = new StringBuilder("以下内容为企业知识库召回结果：");
        for (Document doc : docs) {
            ragText.append(ragPromptService.buildAll(doc))
                    .append("\n\n------------------\n\n");
        }
        return ragText.toString();
    }

    private List<Message> buildMessages(String context, String question) {
        return List.of(
                new SystemMessage("""
                        你是一名专业的企业 AI 助手。
                        请优先依据提供的知识库召回内容回答用户问题。
                        如果知识库未检索到相关内容，请明确回复："知识库中未找到相关内容。"
                        不得编造事实，不得引入知识库之外的信息。
                        """),
                new SystemMessage(context),
                new UserMessage(question)
        );
    }

    private List<Double> sampleEmbedding(float[] embedding) {
        int size = Math.min(12, embedding.length);
        List<Double> sample = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            sample.add((double) embedding[i]);
        }
        return sample;
    }

    public record RagDebugRequest(
            String question,
            Integer topK,
            Long knowledgeBaseId
    ) {
    }

    public record RagDebugResult(
            String question,
            String embeddingModel,
            Integer embeddingDimensions,
            List<Double> embeddingSample,
            Integer topK,
            Double similarityThreshold,
            List<RagDebugChunk> chunks,
            String context,
            String answer
    ) {
    }

    public record RagDebugChunk(
            Integer rank,
            Long documentId,
            Long chunkId,
            String fileName,
            Long knowledgeBaseId,
            Integer pageNumber,
            Integer chunkIndex,
            Double score,
            String content
    ) {
    }
}
