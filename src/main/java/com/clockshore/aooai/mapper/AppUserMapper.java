package com.clockshore.aooai.mapper;

import com.clockshore.aooai.entity.AppUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface AppUserMapper {

    int insert(AppUser user);

    int update(AppUser user);

    int deleteById(@Param("id") Long id);

    AppUser selectById(@Param("id") Long id);

    List<AppUser> selectAll();

    @Update("""
            UPDATE app_user
            SET status = #{status},
                updated_at = CURRENT_TIMESTAMP(3)
            WHERE id = #{id}
            """)
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Select("""
            SELECT u.id AS userId,
                   COUNT(DISTINCT c.id) AS conversationCount,
                   SUM(CASE WHEN cm.role = 'USER' THEN 1 ELSE 0 END) AS questionCount,
                   COALESCE(SUM(cm.prompt_tokens), 0) AS promptTokens,
                   COALESCE(SUM(cm.completion_tokens), 0) AS completionTokens,
                   COALESCE(SUM(cm.total_tokens), 0) AS totalTokens,
                   MAX(cm.created_at) AS lastAskAt
            FROM app_user u
            LEFT JOIN conversation c ON c.user_id = u.id
            LEFT JOIN conversation_message cm ON cm.conversation_id = c.id
            GROUP BY u.id
            """)
    List<Map<String, Object>> selectUserUsageStats();

    AppUser selectByUsername(@Param("username") String username);

    String selectPrimaryRoleCodeByUserId(@Param("userId") Long userId);

}
