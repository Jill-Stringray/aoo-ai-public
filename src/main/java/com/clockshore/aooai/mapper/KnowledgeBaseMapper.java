package com.clockshore.aooai.mapper;

import com.clockshore.aooai.entity.KnowledgeBase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface KnowledgeBaseMapper {

    @Select("""
            SELECT *
            FROM knowledge_base
            WHERE status != 'DELETED'
            ORDER BY updated_at DESC, id DESC
            """)
    List<KnowledgeBase> selectAllActive();

    @Select("""
            SELECT *
            FROM knowledge_base
            WHERE id = #{id}
            """)
    KnowledgeBase selectById(@Param("id") Long id);

    @Update("""
            UPDATE knowledge_base
            SET name = #{name}
            WHERE id = #{id}
              AND status != 'DELETED'
            """)
    int updateName(@Param("id") Long id, @Param("name") String name);

    @Update("""
            UPDATE knowledge_base
            SET status = 'DELETED'
            WHERE id = #{id}
              AND status != 'DELETED'
            """)
    int markDeleted(@Param("id") Long id);
}
