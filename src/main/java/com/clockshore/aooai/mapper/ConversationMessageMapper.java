package com.clockshore.aooai.mapper;

import com.clockshore.aooai.dto.message.ChatMessage;
import com.clockshore.aooai.entity.ConversationMessage;
import java.util.ArrayList;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.util.CollectionUtils;

@Mapper
public interface ConversationMessageMapper {

    @Insert("""
            INSERT INTO conversation_message (
                conversation_id, message_key, parent_message_id, role, content, model_name,
                prompt_tokens, completion_tokens, total_tokens, latency_ms, finish_reason
            )
            VALUES (
                #{conversationId}, #{messageKey}, #{parentMessageId}, #{role}, #{content}, #{modelName},
                #{promptTokens}, #{completionTokens}, #{totalTokens}, #{latencyMs}, #{finishReason}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    Long insert(ConversationMessage message);

    @Select("""
            SELECT id, conversation_id AS conversationId, message_key AS messageKey,
                   parent_message_id AS parentMessageId, role, content, model_name AS modelName,
                   prompt_tokens AS promptTokens, completion_tokens AS completionTokens,
                   total_tokens AS totalTokens, latency_ms AS latencyMs,
                   finish_reason AS finishReason, created_at AS createdAt
            FROM conversation_message
            WHERE message_key = #{messageKey}
            """)
    ConversationMessage selectByMessageKey(@Param("messageKey") String messageKey);

    @Select("""
            SELECT id, conversation_id AS conversationId, message_key AS messageKey,
                   parent_message_id AS parentMessageId, role, content, model_name AS modelName,
                   prompt_tokens AS promptTokens, completion_tokens AS completionTokens,
                   total_tokens AS totalTokens, latency_ms AS latencyMs,
                   finish_reason AS finishReason, created_at AS createdAt
            FROM conversation_message
            WHERE conversation_id = #{conversationId}
            ORDER BY created_at ASC, id ASC
            """)
    List<ConversationMessage> selectByConversationId(@Param("conversationId") Long conversationId);

    @Update("""
            UPDATE conversation_message
            SET parent_message_id = #{parentMessageId},
                role = #{role},
                content = #{content},
                model_name = #{modelName},
                prompt_tokens = #{promptTokens},
                completion_tokens = #{completionTokens},
                total_tokens = #{totalTokens},
                latency_ms = #{latencyMs},
                finish_reason = #{finishReason}
            WHERE message_key = #{messageKey}
            """)
    int updateByMessageKey(ConversationMessage message);

    @Delete("DELETE FROM conversation_message WHERE message_key = #{messageKey}")
    int deleteByMessageKey(@Param("messageKey") String messageKey);

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
