package com.clockshore.aooai.mapper;

import com.clockshore.aooai.chat.ChatMessage;
import com.clockshore.aooai.entity.ConversationMessage;
import org.apache.ibatis.annotations.*;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@Mapper
public interface ConversationMessageMapper {

    @Insert("""
            INSERT INTO conversation_message (message_key, conversation_id, content, token_count, role)
            VALUES (#{messageKey}, #{conversationId}, #{content}, #{tokenCount}, #{role})
            """)
    Long insert(ConversationMessage message);

    @Select("""
            SELECT message_key, conversation_id AS conversationId, content, token_count AS tokenCount, created_at AS createdAt, role
            FROM conversation_message
            WHERE message_key = #{messageKey}
            """)
    ConversationMessage selectByMessageKey(@Param("messageKey") String messageKey);

    @Select("""
            SELECT message_key, conversation_id AS conversationId, content, token_count AS tokenCount, created_at AS createdAt, role
            FROM conversation_message
            WHERE conversation_id = #{conversationId}
            ORDER BY created_at ASC, message_key ASC
            """)
    List<ConversationMessage> selectByConversationId(@Param("conversationId") Long conversationId);

    @Update("""
            UPDATE conversation_message
            SET content = #{content},
                token_count = #{tokenCount},
                role = #{role}
            WHERE message_key = #{messageKey}
            """)
    int updateByMessageKey(ConversationMessage message);


    @Delete("DELETE FROM conversation_message WHERE message_key = #{messageKey}")
    int messageKey(@Param("messageKey") String messageKey);

    default List<ChatMessage> toChatMessages(List<ConversationMessage> conversationMessages) {
        if (CollectionUtils.isEmpty(conversationMessages)) {
            return List.of();
        }

        List<ChatMessage> messages = new ArrayList<>();
        for (ConversationMessage conversationMessage : conversationMessages) {
            ChatMessage message = toChatMessage(conversationMessage);
            if (message != null) {
                messages.add(message);
            }
        }
        return messages;
    }

    default ChatMessage toChatMessage(ConversationMessage conversationMessage) {
        if (conversationMessage == null) {
            return null;
        }

        return ChatMessage.builder()
                .role(conversationMessage.getRole())
                .content(conversationMessage.getContent())
                .build();
    }
}
