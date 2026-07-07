package com.clockshore.aooai.dto.message;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ChatRequestVO {
    private Long conversationId;
    private Long userId;
}
