package com.clockshore.aooai.dto.message;

import lombok.*;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ChatRequest {

    //前端请求体
    private Long conversationId;
    private String conversationKey;
    private String message;
    private Long userId;
    private String model;
    private Double temperature;
    private Boolean stream;
    //可选携带数据
    private List<ChatMessage> messages;

    public String resolveMessage() {
        if (StringUtils.hasText(message)) {
            return message.trim();
        }

        if (!CollectionUtils.isEmpty(messages)) {
            for (int i = messages.size() - 1; i >= 0; i--) {
                ChatMessage chatMessage = messages.get(i);
                if (chatMessage == null) {
                    continue;
                }

                if (!StringUtils.hasText(chatMessage.getContent())) {
                    continue;
                }

                if (!StringUtils.hasText(chatMessage.getRole()) || "user".equalsIgnoreCase(chatMessage.getRole().trim())) {
                    return chatMessage.getContent().trim();
                }
            }
        }

        return null;
    }
}
