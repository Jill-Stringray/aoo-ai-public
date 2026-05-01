package com.clockshore.aooai.chat;

import com.clockshore.aooai.Entity.ConversationMessage;
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
                .id(message.getId())
                .role(message.getRole())
                .content(message.getContent())
                .build();
    }
}
