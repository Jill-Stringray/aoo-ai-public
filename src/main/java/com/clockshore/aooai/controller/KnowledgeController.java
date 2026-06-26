package com.clockshore.aooai.controller;

import com.clockshore.aooai.common.ApiResponse;
import com.clockshore.aooai.service.KnowledgeService;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;


@RestController
@RequestMapping("/api/v1/knowledge")
public class KnowledgeController {

    @Resource
    private KnowledgeService knowledgeService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<KnowledgeService.IngestResult>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        KnowledgeService.IngestResult result = knowledgeService.ingestAndIndex(file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("KNOWLEDGE_DOCUMENT_INDEXED", "文档已完成上传、解析和索引", result));
    }
}
