package com.clockshore.aooai.controller;

import com.clockshore.aooai.domain.model.DocumentInfo;
import com.clockshore.aooai.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping
    public DocumentInfo create(@RequestBody Map<String, String> payload) {
        return documentService.create(Long.valueOf(payload.get("knowledgeBaseId")), payload.get("fileName"));
    }

    @GetMapping
    public List<DocumentInfo> list(@RequestParam Long knowledgeBaseId) {
        return documentService.listByKnowledgeBase(knowledgeBaseId);
    }

    @PatchMapping("/{documentId}/status")
    public DocumentInfo status(@PathVariable Long documentId, @RequestBody Map<String, String> payload) {
        return documentService.updateStatus(documentId, payload.get("status"));
    }
}
