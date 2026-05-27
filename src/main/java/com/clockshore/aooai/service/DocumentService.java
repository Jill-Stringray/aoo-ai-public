package com.clockshore.aooai.service;

import com.clockshore.aooai.domain.enums.DocumentStatus;
import com.clockshore.aooai.domain.model.DocumentInfo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class DocumentService {
    private final AtomicLong idGen = new AtomicLong(1);
    private final List<DocumentInfo> data = new ArrayList<>();

    public DocumentInfo create(Long knowledgeBaseId, String fileName) {
        DocumentInfo doc = DocumentInfo.builder()
                .id(idGen.getAndIncrement())
                .knowledgeBaseId(knowledgeBaseId)
                .fileName(fileName)
                .status(DocumentStatus.UPLOADING)
                .build();
        data.add(doc);
        return doc;
    }

    public List<DocumentInfo> listByKnowledgeBase(Long knowledgeBaseId) {
        return data.stream().filter(it -> it.getKnowledgeBaseId().equals(knowledgeBaseId)).toList();
    }

    public DocumentInfo updateStatus(Long documentId, String status) {
        DocumentStatus target = DocumentStatus.valueOf(status);
        for (DocumentInfo item : data) {
            if (item.getId().equals(documentId)) {
                item.setStatus(target);
                return item;
            }
        }
        return null;
    }
}
