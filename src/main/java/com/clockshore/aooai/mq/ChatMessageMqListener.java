package com.clockshore.aooai.mq;

import com.clockshore.aooai.Utility.ToJsonUtil;
import com.clockshore.aooai.entity.ConversationMessage;
import com.clockshore.aooai.entity.DocumentChunk;
import com.clockshore.aooai.mapper.ConversationMessageMapper;
import com.clockshore.aooai.mapper.DocumentChunkMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.ai.vectorstore.VectorStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
public class ChatMessageMqListener {

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private VectorStore vectorStore;

    @Resource
    private ToJsonUtil toJsonUtil;

    @Resource
    private DocumentChunkMapper documentChunkMapper;

    @Resource
    private ConversationMessageMapper conversationMessageMapper;

    @Transactional
    @RabbitListener(queues = ChatMqConfig.CHAT_QUEUE)
    public void chatMessageConsume(String payload) {
        ChatMessageEvent event = parse(payload);
        ConversationMessage conversationMessage = ConversationMessage.builder()
                .conversationId(event.getConversationId())
                .messageKey(event.getMessageKey())
                .parentMessageId(event.getParentMessageId())
                .role(event.getRole())
                .content(event.getContent())
                .modelName(event.getModelName())
                .promptTokens(defaultZero(event.getPromptTokens()))
                .completionTokens(defaultZero(event.getCompletionTokens()))
                .totalTokens(defaultZero(event.getTotalTokens()))
                .latencyMs(event.getLatencyMs())
                .finishReason(event.getFinishReason())
                .build();
        conversationMessageMapper.insert(conversationMessage);
    }

    @RabbitListener(queues = ChatMqConfig.EMBEDDING_QUEUE)
    public void embeddingConsume(Long documentId){
        // 1 查询待向量化 Chunk
        List<DocumentChunk> chunks =
                documentChunkMapper.selectNewByDocumentId(documentId);

        // 2 转换成 Spring AI Document
        List<Document> docs =
                convertToDocuments(chunks);

        // 3 写入向量库
        vectorStore.add(docs);

        // 4 更新状态
        documentChunkMapper.updateStatusByDocumentId(
                documentId,
                "EMBEDDED"
        );
    }

    private Integer defaultZero(Integer value) {
        return value == null ? 0 : value;
    }

    private ChatMessageEvent parse(String payload) {
        try {
            return objectMapper.readValue(payload, ChatMessageEvent.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("parse chat message event failed", e);
        }
    }

    //转化为document类型插入vector
    private List<Document> convertToDocuments(
            List<DocumentChunk> chunks) {

        return chunks.stream()
                .map(this::convertToDocument)
                .toList();
    }


    private Document convertToDocument(DocumentChunk chunk) {

        Map<String, Object> metadata;


        metadata = toJsonUtil.toMap(chunk.getMetadata());

        // 补充一些检索时常用字段
        metadata.put("chunkId", chunk.getId());
        metadata.put("chunkKey", chunk.getChunkKey());
        metadata.put("documentId", chunk.getDocumentId());
        metadata.put("knowledgeBaseId", chunk.getKnowledgeBaseId());

        if (chunk.getPageNo() != null) {
            metadata.put("pageNo", chunk.getPageNo());
        }

        if (chunk.getSectionTitle() != null) {
            metadata.put("sectionTitle", chunk.getSectionTitle());
        }

        return new Document(
                chunk.getContent(),
                metadata
        );
    }


}
