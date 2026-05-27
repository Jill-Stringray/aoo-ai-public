package com.clockshore.aooai.controller;

import com.clockshore.aooai.domain.enums.DocumentStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    @PostMapping("/upload")
    public Map<String, Object> upload() {
        return Map.of(
                "documentId", 1,
                "status", DocumentStatus.UPLOADING,
                "message", "文档上传接口已预留，后续接入 OSS + 异步解析任务"
        );
    }

    @GetMapping("/{documentId}/status")
    public Map<String, Object> status(@PathVariable Long documentId) {
        return Map.of("documentId", documentId, "status", DocumentStatus.PARSING);
    }
}
