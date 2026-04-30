package com.clockshore.aooai.Mapper;

import com.clockshore.aooai.Entity.ConversationMessage;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ConversationMessageMapper {

    @Insert("""
            INSERT INTO conversation_message (conversation_id, content, token_count, message_order, role)
            VALUES (#{conversationId}, #{content}, #{tokenCount}, #{messageOrder}, #{role})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    Long insert(ConversationMessage message);

    @Select("""
            SELECT id, conversation_id AS conversationId, content, token_count AS tokenCount,
                   message_order AS messageOrder, created_at AS createdAt, role
            FROM conversation_message
            WHERE id = #{id}
            """)
    ConversationMessage selectById(@Param("id") Long id);

    @Select("""
            SELECT id, conversation_id AS conversationId, content, token_count AS tokenCount,
                   message_order AS messageOrder, created_at AS createdAt, role
            FROM conversation_message
            WHERE conversation_id = #{conversationId}
            ORDER BY message_order ASC
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

    @Select("""
            SELECT COALESCE(MAX(message_order), 0) + 1
            FROM conversation_message
            WHERE conversation_id = #{conversationId}
            FOR UPDATE
            """)
    long nextOrder(@Param("conversationId") Long conversationId);

    @Delete("DELETE FROM conversation_message WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}