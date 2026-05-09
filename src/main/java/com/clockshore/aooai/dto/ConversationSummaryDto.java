package com.clockshore.aooai.dto;

import com.clockshore.aooai.entity.Conversation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationSummaryDto {

    private Long id;
    private String conversationKey;
    private String title;
    private LocalDateTime updatedAt;

    public static ConversationSummaryDto from(Conversation c) {
        return ConversationSummaryDto.builder()
                .id(c.getId())
                .conversationKey(c.getConversationKey())
                .title(c.getTitle())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
