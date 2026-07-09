package com.clockshore.aooai.dto.message;

import com.clockshore.aooai.entity.Conversation;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ConversationVO {

    /**
     * 会话ID
     */
    private Long conversationId;

    /**
     * 会话标题
     */
    private String title;

    /**
     * 最后更新时间
     */
    private LocalDateTime updateTime;

    public static ConversationVO from(Conversation conversation) {
        return ConversationVO.builder()
                .conversationId(conversation.getId())
                .title(conversation.getTitle())
                .updateTime(conversation.get)
                .build();
    }
}