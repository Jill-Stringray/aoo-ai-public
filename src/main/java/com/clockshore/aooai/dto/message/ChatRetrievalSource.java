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
    private Long documentId;
    private Long chunkId;
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
                .documentId(source.getDocumentId())
                .chunkId(source.getChunkId())
                .score(source.getScore())
                .rerankScore(source.getRerankScore())
                .quoteText(source.getQuoteText())
                .build();
    }
}
