package com.clockshore.aooai.mapper;

import com.clockshore.aooai.entity.DocumentChunk;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DocumentChunkMapper {

    int insert(DocumentChunk chunk);

    DocumentChunk selectById(@Param("id") Long id);

    List<DocumentChunk> selectByDocumentId(
            @Param("documentId") Long documentId
    );

    int updateStatus(
            @Param("id") Long id,
            @Param("status") String status
    );

    int updateVectorRef(
            @Param("id") Long id,
            @Param("vectorRef") String vectorRef
    );

    int deleteByDocumentId(
            @Param("documentId") Long documentId
    );
}