package com.clockshore.aooai.mapper;

import com.clockshore.aooai.entity.Conversation;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ConversationMapper {

    @Insert("""
            INSERT INTO conversation (conversation_key, user_id, session_id, title, status)
            VALUES (#{conversationKey}, #{userId}, #{sessionId}, #{title} , #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    Long insert(Conversation conversation);

    @Select("""
            SELECT id, conversation_key AS conversationKey, user_id AS userId, session_id AS sessionId,
                   title, status, created_at AS createdAt, updated_at AS updatedAt
            FROM conversation
            WHERE id = #{id}
            """)
    Conversation selectById(@Param("id") Long id);

    @Select("""
    SELECT id, conversation_key AS conversationKey, user_id AS userId, session_id AS sessionId,
           title, status, created_at AS createdAt, updated_at AS updatedAt
    FROM conversation
    WHERE conversation_key = #{conversationKey}
            """)
    Conversation selectByConversationKey(@Param("conversationKey") String conversationKey);

    @Select("""
            SELECT id, conversation_key AS conversationKey, user_id AS userId, session_id AS sessionId,
                   title, status, created_at AS createdAt, updated_at AS updatedAt
            FROM conversation
            WHERE user_id = #{userId}
            ORDER BY updated_at DESC
            """)
    List<Conversation> selectByUserId(@Param("userId") Long userId);

    @Update("""
            UPDATE conversation
            SET title = #{title},
                status = #{status},
                session_id = #{sessionId},
                user_id = #{userId}
            WHERE id = #{id}
            """)
    int updateById(Conversation conversation);

    @Delete("DELETE FROM conversation WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}
