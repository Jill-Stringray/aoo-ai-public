package com.clockshore.aooai.chat;

import lombok.Getter;
import lombok.Setter;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

@Getter
@Setter
public class ChatRequest {

    private String message;
    private String model;
    private Double temperature;
    private Boolean stream;
    private List<Message> messages;

    public String resolveMessage() {
        if (StringUtils.hasText(message)) {
            return message.trim();
        }

        if (!CollectionUtils.isEmpty(messages)) {
            for (int i = messages.size() - 1; i >= 0; i--) {
                Message chatMessage = messages.get(i);
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

    @Getter
    @Setter
    public static class Message {
        private String role;
        private String content;
    }
}
