package com.clockshore.aooai.mapper;

import com.clockshore.aooai.entity.Conversation;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ConversationMapper {

    @Insert("""
            INSERT INTO conversation (conversation_key, user_id, title, status)
            VALUES (#{conversationKey}, #{userId}, #{title} , #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    Long insert(Conversation conversation);

    @Select("""
            SELECT id, conversation_key AS conversationKey, user_id AS userId,
                   title, status, is_favorite AS isFavorite,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM conversation
            WHERE id = #{id}
            """)
    Conversation selectById(@Param("id") Long id);

    @Select("""
            SELECT id, conversation_key AS conversationKey, user_id AS userId,
                   title, status, is_favorite AS isFavorite,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM conversation
            WHERE id = #{id}
              AND user_id = #{userId}
              AND UPPER(TRIM(status)) = 'ACTIVE'
            """)
    Conversation selectByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Select("""
    SELECT id, conversation_key AS conversationKey, user_id AS userId,
           title, status, is_favorite AS isFavorite,
           created_at AS createdAt, updated_at AS updatedAt
    FROM conversation
    WHERE conversation_key = #{conversationKey}
            """)
    Conversation selectByConversationKey(@Param("conversationKey") String conversationKey);

    @Select("""
            SELECT id, conversation_key AS conversationKey, user_id AS userId,
                   title, status, is_favorite AS isFavorite,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM conversation
            WHERE user_id = #{userId} AND UPPER(TRIM(status)) = 'ACTIVE'
              AND (#{favoriteOnly} = 0 OR is_favorite = 1)
            ORDER BY updated_at DESC
            """)
    List<Conversation> selectByUserId(
            @Param("userId") Long userId,
            @Param("favoriteOnly") Boolean favoriteOnly);

    @Update("""
            UPDATE conversation
            SET title = #{title},
                status = #{status},
                user_id = #{userId}
            WHERE id = #{id}
            """)
    int updateById(Conversation conversation);

    @Update("""
            UPDATE conversation
            SET status = #{status},
                updated_at = CURRENT_TIMESTAMP(3)
            WHERE id = #{id}
              AND user_id = #{userId}
              AND UPPER(TRIM(status)) = 'ACTIVE'
            """)
    int updateStatusForUser(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("status") String status);

    @Update("""
            UPDATE conversation
            SET title = CASE
                    WHEN title IS NULL OR title = '' OR title = '新对话' THEN #{title}
                    ELSE title
                END,
                updated_at = CURRENT_TIMESTAMP(3)
            WHERE id = #{id}
            """)
    int touchAfterUserMessage(
            @Param("id") Long id,
            @Param("title") String title);

    @Update("""
            UPDATE conversation
            SET title = #{title},
                updated_at = CURRENT_TIMESTAMP(3)
            WHERE id = #{id}
              AND user_id = #{userId}
              AND UPPER(TRIM(status)) = 'ACTIVE'
            """)
    int updateTitleForUser(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("title") String title);

    @Update("""
            UPDATE conversation
            SET is_favorite = #{favorite},
                updated_at = CURRENT_TIMESTAMP(3)
            WHERE id = #{id}
              AND user_id = #{userId}
              AND UPPER(TRIM(status)) = 'ACTIVE'
            """)
    int updateFavoriteForUser(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("favorite") Boolean favorite);

}
