package com.clockshore.aooai.chat;

import com.clockshore.aooai.entity.ConversationMessage;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ConversationMessageDto {
    private Long id;
    private String role;
    private String content;

    public static ConversationMessageDto from(ConversationMessage message) {
        return ConversationMessageDto.builder()
                .id(message.getMessageKey())
                .role(message.getRole())
                .content(message.getContent())
                .build();
    }
}
