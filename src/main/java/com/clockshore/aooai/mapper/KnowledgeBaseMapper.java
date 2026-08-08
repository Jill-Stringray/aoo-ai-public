package com.clockshore.aooai.mapper;

import com.clockshore.aooai.entity.KnowledgeBase;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
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

    @Insert("""
            INSERT INTO knowledge_base (
                kb_code,
                name,
                description,
                visibility,
                status,
                embedding_model,
                retrieval_config
            ) VALUES (
                #{kbCode},
                #{name},
                #{description},
                #{visibility},
                #{status},
                #{embeddingModel},
                #{retrievalConfig}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(KnowledgeBase knowledgeBase);

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
