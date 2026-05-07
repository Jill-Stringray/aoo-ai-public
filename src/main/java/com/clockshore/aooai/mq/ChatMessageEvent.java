package com.clockshore.aooai.mq;

import lombok.*;

import java.io.Serializable;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageEvent implements Serializable {

    private Long conversationId;
    private String messageKey;
    private Long userId;
    private String role;
    private String content;

}
