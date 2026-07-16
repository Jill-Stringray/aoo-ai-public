package com.clockshore.aooai.controller;

import com.clockshore.aooai.common.ApiResponse;
import com.clockshore.aooai.service.KnowledgeService;
import jakarta.annotation.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;


@RestController
@RequestMapping("/api/v1/knowledge")
public class KnowledgeController {

    @Resource
    private KnowledgeService knowledgeService;

    @GetMapping("/stats")
    public ApiResponse<KnowledgeService.KnowledgeStats> stats() {
        return ApiResponse.success("KNOWLEDGE_STATS_FETCHED", "知识库统计已获取", knowledgeService.getKnowledgeStats());
    }

    @GetMapping("/bases")
    public ApiResponse<java.util.List<KnowledgeService.KnowledgeBaseVO>> bases() {
        return ApiResponse.success("KNOWLEDGE_BASES_FETCHED", "知识库列表已获取", knowledgeService.listKnowledgeBases());
    }

    @PostMapping("/bases")
    public ApiResponse<KnowledgeService.KnowledgeBaseVO> createKnowledgeBase(
            @RequestBody KnowledgeService.CreateKnowledgeBaseRequest request) {
        return ApiResponse.success(
                "KNOWLEDGE_BASE_CREATED",
                "知识库已创建",
                knowledgeService.createKnowledgeBase(request)
        );
    }

    @PutMapping("/bases/{knowledgeBaseId}/name")
    public ApiResponse<Void> renameKnowledgeBase(
            @PathVariable("knowledgeBaseId") Long knowledgeBaseId,
            @RequestBody Map<String, String> request) {
        knowledgeService.renameKnowledgeBase(knowledgeBaseId, request.get("name"));
        return ApiResponse.success("KNOWLEDGE_BASE_RENAMED", "知识库名称已修改", null);
    }

    @DeleteMapping("/bases/{knowledgeBaseId}")
    public ApiResponse<Void> deleteKnowledgeBase(@PathVariable("knowledgeBaseId") Long knowledgeBaseId) {
        knowledgeService.deleteKnowledgeBase(knowledgeBaseId);
        return ApiResponse.success("KNOWLEDGE_BASE_DELETED", "知识库已删除", null);
    }

    @GetMapping("/bases/{knowledgeBaseId}/documents")
    public ApiResponse<java.util.List<KnowledgeService.KnowledgeDocumentVO>> documents(
            @PathVariable("knowledgeBaseId") Long knowledgeBaseId) {
        return ApiResponse.success(
                "KNOWLEDGE_DOCUMENTS_FETCHED",
                "知识库文档列表已获取",
                knowledgeService.listDocuments(knowledgeBaseId)
        );
    }

    @GetMapping("/documents/{documentId}/content")
    public ApiResponse<KnowledgeService.DocumentContentVO> documentContent(
            @PathVariable("documentId") Long documentId) {
        return ApiResponse.success(
                "KNOWLEDGE_DOCUMENT_CONTENT_FETCHED",
                "文档内容已获取",
                knowledgeService.getDocumentContent(documentId)
        );
    }

    @DeleteMapping("/documents/{documentId}")
    public ApiResponse<Void> deleteDocument(@PathVariable("documentId") Long documentId) {
        knowledgeService.deleteDocument(documentId);
        return ApiResponse.success("KNOWLEDGE_DOCUMENT_DELETED", "文档已删除", null);
    }

    @GetMapping("/documents/{documentId}/file")
    public ResponseEntity<org.springframework.core.io.Resource> documentFile(@PathVariable("documentId") Long documentId) {
        KnowledgeService.OriginalDocumentResource original = knowledgeService.getOriginalDocument(documentId);
        String contentType = original.mimeType() == null || original.mimeType().isBlank()
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : original.mimeType();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(original.fileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(original.resource());
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

    @PostMapping("/bases/{knowledgeBaseId}/upload")
    public ApiResponse<KnowledgeService.IngestResult> uploadToKnowledgeBase(
            @PathVariable("knowledgeBaseId") Long knowledgeBaseId,
            @RequestParam("file") MultipartFile file) throws IOException {

        KnowledgeService.IngestResult result =
                knowledgeService.ingestAndPublishEmbeddingTask(knowledgeBaseId, file);

        return ApiResponse.success(
                "KNOWLEDGE_DOCUMENT_EMBEDDING_QUEUED",
                "文档已完成上传、解析和切片，已提交 MQ 异步索引",
                result);
    }
}
