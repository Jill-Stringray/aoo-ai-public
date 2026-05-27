package com.clockshore.aooai.service;

import com.clockshore.aooai.domain.model.DocumentInfo;

import java.util.List;

public interface DocumentService {
    DocumentInfo create(Long knowledgeBaseId, String fileName);

    List<DocumentInfo> listByKnowledgeBase(Long knowledgeBaseId);

    DocumentInfo updateStatus(Long documentId, String status);
}
