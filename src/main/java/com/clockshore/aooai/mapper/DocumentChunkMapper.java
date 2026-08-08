package com.clockshore.aooai.mapper;

import com.clockshore.aooai.entity.DocumentChunk;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DocumentChunkMapper {

    int insert(DocumentChunk chunk);

    int batchInsert(
            @Param("chunks")
            List<DocumentChunk> chunks
    );

    int markEmbedding(
            @Param("documentId") Long documentId
    );

    List<DocumentChunk> selectEmbeddingByDocumentId(
            @Param("documentId")
            Long documentId
    );

    DocumentChunk selectById(@Param("id") Long id);

    List<DocumentChunk> selectNewByDocumentId(
            @Param("documentId") Long documentId
    );

    List<DocumentChunk> selectByDocumentId(
            @Param("documentId") Long documentId
    );

    int updateStatus(
            @Param("id") Long id,
            @Param("status") String status
    );

    int updateStatusByDocumentId(
            @Param("documentId") Long documentId,
            @Param("status") String status
    );

    int updateVectorRef(
            @Param("id") Long id,
            @Param("vectorRef") String vectorRef
    );

    int deleteByDocumentId(
            @Param("documentId") Long documentId
    );

    int markDeletedByDocumentId(
            @Param("documentId") Long documentId
    );

    int markDeletedByKnowledgeBaseId(
            @Param("knowledgeBaseId") Long knowledgeBaseId
    );

    int findHash(
            @Param("contentHash") String contentHash
    );

    int countSearchable();


}
