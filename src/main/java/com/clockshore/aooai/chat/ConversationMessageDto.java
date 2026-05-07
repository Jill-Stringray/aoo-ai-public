package com.clockshore.aooai.chat;

import com.clockshore.aooai.entity.ConversationMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMessageDto {

    private String role;
    private String content;

    public static ConversationMessageDto from(ConversationMessage m) {
        if (m == null) {
            return null;
        }
        return ConversationMessageDto.builder()
                .role(m.getRole())
                .content(m.getContent() != null ? m.getContent() : "")
                .build();
    }
}
