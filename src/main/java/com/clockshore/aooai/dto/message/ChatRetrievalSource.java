package com.clockshore.aooai.dto.message;

import com.clockshore.aooai.entity.MessageRetrievalSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRetrievalSource {

    private Integer index;
    private Long knowledgeBaseId;
    private String knowledgeBaseName;
    private Long documentId;
    private String fileName;
    private Long chunkId;
    private Integer pageNumber;
    private Double score;
    private Double rerankScore;
    private String quoteText;

    public static ChatRetrievalSource from(MessageRetrievalSource source) {
        if (source == null) {
            return null;
        }

        return ChatRetrievalSource.builder()
                .index(source.getRankNo())
                .knowledgeBaseId(source.getKnowledgeBaseId())
                .knowledgeBaseName(source.getKnowledgeBaseName())
                .documentId(source.getDocumentId())
                .fileName(source.getFileName())
                .chunkId(source.getChunkId())
                .pageNumber(source.getPageNumber())
                .score(source.getScore())
                .rerankScore(source.getRerankScore())
                .quoteText(source.getQuoteText())
                .build();
    }
}
