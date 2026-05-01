package com.clockshore.aooai.Mapper;

import com.clockshore.aooai.Entity.ConversationMessage;
import com.clockshore.aooai.chat.ChatRequest;
import org.apache.ibatis.annotations.*;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@Mapper
public interface ConversationMessageMapper {

    @Insert("""
            INSERT INTO conversation_message (conversation_id, content, token_count, role)
            VALUES (#{conversationId}, #{content}, #{tokenCount}, #{role})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    Long insert(ConversationMessage message);

    @Select("""
            SELECT id, conversation_id AS conversationId, content, token_count AS tokenCount, created_at AS createdAt, role
            FROM conversation_message
            WHERE id = #{id}
            """)
    ConversationMessage selectById(@Param("id") Long id);

    @Select("""
            SELECT id, conversation_id AS conversationId, content, token_count AS tokenCount, created_at AS createdAt, role
            FROM conversation_message
            WHERE conversation_id = #{conversationId}
            ORDER BY conversation_id ASC
            """)
    List<ConversationMessage> selectByConversationId(@Param("conversationId") Long conversationId);

    @Update("""
            UPDATE conversation_message
            SET content = #{content},
                token_count = #{tokenCount},
                role = #{role}
            WHERE id = #{id}
            """)
    int updateById(ConversationMessage message);


    @Delete("DELETE FROM conversation_message WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    default List<ChatRequest.Message> toChatRequestMessages(List<ConversationMessage> conversationMessages) {
        if (CollectionUtils.isEmpty(conversationMessages)) {
            return List.of();
        }

        List<ChatRequest.Message> messages = new ArrayList<ChatRequest.Message>();
        for (ConversationMessage conversationMessage : conversationMessages) {
            ChatRequest.Message message = toChatRequestMessage(conversationMessage);
            if (message != null) {
                messages.add(message);
            }
        }
        return messages;
    }

    default ChatRequest.Message toChatRequestMessage(ConversationMessage conversationMessage) {
        if (conversationMessage == null) {
            return null;
        }

        return ChatRequest.Message.builder()
                .role(conversationMessage.getRole())
                .content(conversationMessage.getContent())
                .build();
    }
}
