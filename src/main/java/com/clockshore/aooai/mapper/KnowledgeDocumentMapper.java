package com.clockshore.aooai.mapper;

import com.clockshore.aooai.entity.KnowledgeDocument;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface KnowledgeDocumentMapper {

    int insert(KnowledgeDocument document);

    KnowledgeDocument selectById(@Param("id") Long id);

    int updateStatus(
            @Param("id") Long id,
            @Param("status") String status
    );

    int deleteById(@Param("id") Long id);

    int markDeletedByKnowledgeBaseId(@Param("knowledgeBaseId") Long knowledgeBaseId);

    int countAll();

    int countByStatus(@Param("status") String status);

    List<KnowledgeDocument> selectByKnowledgeBaseId(@Param("knowledgeBaseId") Long knowledgeBaseId);
}
