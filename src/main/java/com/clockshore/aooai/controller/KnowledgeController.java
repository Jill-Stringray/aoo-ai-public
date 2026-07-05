package com.clockshore.aooai.controller;

import com.clockshore.aooai.common.ApiResponse;
import com.clockshore.aooai.enumType.ResultCode;
import com.clockshore.aooai.service.KnowledgeService;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping("/stats")
    public ApiResponse<KnowledgeService.KnowledgeStats> stats() {
        return ApiResponse.success("KNOWLEDGE_STATS_FETCHED", "知识库统计已获取", knowledgeService.getKnowledgeStats());
    }

    /**
     * 上传文件接口
     * 上传之后发布MQ进入embedding队列
     */
    @PostMapping("/upload")
    public ApiResponse<KnowledgeService.IngestResult> upload(
            @RequestParam("file") MultipartFile file) throws IOException {

        KnowledgeService.IngestResult result =
                knowledgeService.ingestAndPublishEmbeddingTask(file);

        return ApiResponse.success(
                "KNOWLEDGE_DOCUMENT_EMBEDDING_QUEUED",
                "文档已完成上传、解析和切片，已提交 MQ 异步索引",
                result);
    }
}
