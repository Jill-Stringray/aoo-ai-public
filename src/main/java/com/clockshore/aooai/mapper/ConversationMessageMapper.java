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
import org.springframework.ai.chat.messages.Message;
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
            SELECT id, conversation_id, message_key, parent_message_id, role, content, model_name,
                   prompt_tokens, completion_tokens ,
                   total_tokens, latency_ms ,
                   finish_reason, created_at
            FROM conversation_message
            WHERE message_key = #{messageKey}
            """)
    ConversationMessage selectByMessageKey(@Param("messageKey") String messageKey);

    @Select("""
            SELECT id,
                   conversation_id,
                   message_key,
                   parent_message_id,
                   role,
                   content,
                   model_name,
                   prompt_tokens,
                   completion_tokens,
                   total_tokens,
                   latency_ms,
                   finish_reason,
                   created_at
            FROM conversation_message
            WHERE conversation_id = #{conversationId}
            ORDER BY created_at ASC, id ASC
        """)
    List<ConversationMessage> selectByConversationId(
            @Param("conversationId") Long conversationId
    );

    @Select("""
         SELECT cm.id,
                cm.conversation_id,
                cm.message_key,
                cm.parent_message_id,
                cm.role,
                cm.content,
                cm.model_name,
                cm.prompt_tokens,
                cm.completion_tokens,
                cm.total_tokens,
                cm.latency_ms,
                cm.finish_reason,
                cm.created_at
            FROM conversation_message cm
            JOIN conversation c
                ON cm.conversation_id = c.id
            WHERE c.user_id = #{userId}
            AND cm.conversation_id = #{conversationId}
            ORDER BY cm.created_at ASC, cm.id ASC;
         """)
    List<Message> selectByConversationIdAndUserId(
            @Param("conversationId") Long conversationId,
            @Param("UserId")Long UserId
    );

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
