package com.clockshore.aooai.service;

import cn.hutool.crypto.digest.DigestUtil;
import com.clockshore.aooai.Utility.ToJsonUtil;
import com.clockshore.aooai.entity.DocumentChunk;
import com.clockshore.aooai.entity.KnowledgeBase;
import com.clockshore.aooai.entity.KnowledgeDocument;
import com.clockshore.aooai.mapper.DocumentChunkMapper;
import com.clockshore.aooai.mapper.KnowledgeBaseMapper;
import com.clockshore.aooai.mapper.KnowledgeDocumentMapper;
import com.clockshore.aooai.mq.ChatMessageMqProducer;
import dev.langchain4j.data.segment.TextSegment;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;


@Service
@RequiredArgsConstructor
public class KnowledgeService {

    @Resource
    private TokenSplitterService tokenSplitterService;
    @Resource
    private KnowledgeDocumentMapper knowledgeDocumentMapper;
    @Resource
    private KnowledgeBaseMapper knowledgeBaseMapper;
    @Resource
    private ToJsonUtil toJsonUtil;
    @Resource
    private DocumentChunkMapper documentChunkMapper;
    @Resource
    private DocumentReaderService documentReaderService;
    @Resource
    private ChatMessageMqProducer chatMessageMqProducer;

    Tika tika = new Tika();

    private final VectorStore vectorStore;

    //validate使用，检查格式和mime类型是否相等
    private static final Map<String, Set<String>> FILE_TYPE_MAPPING = Map.of(
            "pdf", Set.of("application/pdf"),
            "doc", Set.of("application/msword","application/x-tika-msoffice"),
            "docx", Set.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            "txt", Set.of("text/plain"),
            "md", Set.of("text/plain", "text/markdown")
    );



    /**
     * 完整的 RAG 文档入库链路：上传文件、解析文档、清洗文本、补充元数据、切片、
     * 保存切片、写入 VectorStore，最后更新文档状态为 READY。
     */
    @Transactional
    public IngestResult ingestAndIndex(MultipartFile file) throws IOException {
        IngestResult preparedResult = ingestAndSaveChunks(1L, file);

        knowledgeDocumentMapper.updateStatus(preparedResult.documentId(), "INDEXING");
        int indexedChunkCount = indexNewChunks(preparedResult.documentId());

        return new IngestResult(
                preparedResult.documentId(),
                preparedResult.savedChunkCount(),
                indexedChunkCount
        );
    }

    /**
     * 管理端上传入口：上传并保存切片后，只发布 Embedding MQ 任务，
     * 由异步消费者完成 VectorStore 写入，避免在 HTTP 请求线程内直接向量化。
     */
    @Transactional
    public IngestResult ingestAndPublishEmbeddingTask(MultipartFile file) throws IOException {
        return ingestAndPublishEmbeddingTask(1L, file);
    }

    @Transactional
    public IngestResult ingestAndPublishEmbeddingTask(Long knowledgeBaseId, MultipartFile file) throws IOException {
        IngestResult preparedResult = ingestAndSaveChunks(knowledgeBaseId, file);

        knowledgeDocumentMapper.updateStatus(preparedResult.documentId(), "INDEXING");
        chatMessageMqProducer.publishEmbeddingTask(preparedResult.documentId());

        return new IngestResult(
                preparedResult.documentId(),
                preparedResult.savedChunkCount(),
                0
        );
    }

    /**
     * 完成向量化前的文档准备链路：上传文件、解析文档、清洗文本、补充元数据、
     * 切片并保存 Chunk。异步 MQ 消费者可以基于返回的 documentId 继续写入 VectorStore。
     */
    @Transactional
    public IngestResult ingestAndSaveChunks(MultipartFile file) throws IOException {
        return ingestAndSaveChunks(1L, file);
    }

    @Transactional
    public IngestResult ingestAndSaveChunks(Long knowledgeBaseId, MultipartFile file) throws IOException {
        KnowledgeDocument document = storeUploadedDocument(knowledgeBaseId, file);
        knowledgeDocumentMapper.updateStatus(document.getId(), "PARSING");

        List<Document> docs = documentReaderService.read(Paths.get(document.getStorageUrl()));
        docs = documentReaderService.normalize(docs);
        docs = documentReaderService.enrichMetadata(docs, document);

        knowledgeDocumentMapper.updateStatus(document.getId(), "CHUNKING");
        List<TextSegment> segments = tokenSplitterService.split(docs);
        int savedChunkCount = saveChunks(segments);

        return new IngestResult(document.getId(), savedChunkCount, 0);
    }

    @Transactional
    public Long upload(MultipartFile file) throws IOException {
        return storeUploadedDocument(1L, file).getId();
    }

    /**
     * 此函数只进行导入＆保存文件，不处理
     * 保存后写入mysql中的knowledge_document表来记录
     * 返回KnowledgeDocument
     */
    private KnowledgeDocument storeUploadedDocument(Long knowledgeBaseId, MultipartFile file) throws IOException {
        validate(file);
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(knowledgeBaseId);
        if (knowledgeBase == null || "DELETED".equals(knowledgeBase.getStatus())) {
            throw new IllegalArgumentException("知识库不存在");
        }
        //防止../../../test.pdf
        String originalName = Paths.get(
                Objects.requireNonNull(file.getOriginalFilename()))
                .getFileName()
                .toString();

        String extension = FilenameUtils.getExtension(originalName)
                .toLowerCase(Locale.ROOT);

        Path uploadDir = Paths.get("uploads");

        //若uploads不存在，创建文件夹
        Files.createDirectories(uploadDir);

        //UUID加原文件名的名字
        String u = UUID.randomUUID().toString();
        Path filePath = uploadDir.resolve(u + "_" + originalName);

        try{
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            KnowledgeDocument document = KnowledgeDocument.builder()
                    .knowledgeBaseId(knowledgeBaseId)
                    .documentKey(u)
                    .title(originalName)
                    .fileName(originalName)
                    .fileType(extension)
                    .mimeType(tika.detect(file.getInputStream()))
                    .fileSize(file.getSize())
                    .storageUrl(filePath.toString())
                    .sourceType("UPLOAD")
                    .status("UPLOADED")
                    .build();

            knowledgeDocumentMapper.insert(document);
            return document;
        } catch (Exception ex) {
            Files.deleteIfExists(filePath);
            throw ex;
        }
    }

    public int saveChunks(List<TextSegment> segments){
        if (segments.isEmpty()) {
            return 0;
        }
        List<DocumentChunk> chunks = new ArrayList<>();
        for(TextSegment t : segments) {
            String chunkKey = UUID.randomUUID().toString();
            //再次补充chunkKey 进入metadata
            t.metadata().put("chunkKey",chunkKey);

            String metadataJson = toJsonUtil.toJson(t.metadata().toMap());
            String hash = DigestUtil.sha256Hex(t.text());

            //去重跳过
            if(documentChunkMapper.findHash(hash) > 0){
                continue;
            }

            //太短的文本视为无效信息
            if(t.text().length() < 50){
                continue;
            }

            Long knowledgeBaseId = t.metadata().getLong("knowledgeBaseId");
            DocumentChunk chunk = DocumentChunk.builder()
                    .knowledgeBaseId(knowledgeBaseId == null ? 1L : knowledgeBaseId)
                    .documentId(t.metadata().getLong("documentId"))
                    .chunkKey(chunkKey)
                    .chunkIndex(t.metadata().getInteger("index"))
                    .content(t.text())
                    .contentHash(hash)
                    .tokenCount(t.text().length())
                    .pageNo(t.metadata().getInteger("page_number"))
                    .metadata(metadataJson)
                    .status("NEW")
                    .build();

            chunks.add(chunk);
        }

        //防止chunks无内容时报错
        if(chunks.isEmpty()){
            return 0;
        }
        else{
            return documentChunkMapper.batchInsert(chunks);
        }
    }


    /**
     * 通过MQ发起查询指定文档下未入向量库的切片，转换为 Spring AI Document 后写入 VectorStore。
     * 无重复校验，只在Mysql入表时进行校验document是否重复。
     */
    @Transactional
    public int indexNewChunks(Long documentId) {

        //抢占式消费
        int locked =
                documentChunkMapper.markEmbedding(documentId);

        if (locked == 0) {
            return 0;
        }

        List<DocumentChunk> chunks =
                documentChunkMapper.selectEmbeddingByDocumentId(documentId);

        if (chunks.isEmpty()) {
            return 0;
        }

        vectorStore.add(
                convertToDocuments(chunks)
        );

        documentChunkMapper.updateStatusByDocumentId(
                documentId,
                "EMBEDDED"
        );

        knowledgeDocumentMapper.updateStatus(
                documentId,
                "READY"
        );

        return chunks.size();
    }

    //转化为document类型插入vector
    public List<Document> convertToDocuments(List<DocumentChunk> chunks) {
        return chunks.stream()
                .map(this::convertToDocument)
                .toList();
    }

    private Document convertToDocument(DocumentChunk chunk) {
        Map<String, Object> metadata = toJsonUtil.toMap(chunk.getMetadata());

        metadata.put("chunkId", chunk.getId());
        metadata.put("chunkKey", chunk.getChunkKey());
        metadata.put("documentId", chunk.getDocumentId());
        metadata.put("knowledgeBaseId", chunk.getKnowledgeBaseId());

        if (chunk.getPageNo() != null) {
            metadata.put("pageNo", chunk.getPageNo());
        }

        if (chunk.getSectionTitle() != null) {
            metadata.put("sectionTitle", chunk.getSectionTitle());
        }

        return new Document(chunk.getContent(), metadata);
    }

    public KnowledgeStats getKnowledgeStats() {
        return new KnowledgeStats(
                knowledgeDocumentMapper.countAll(),
                knowledgeDocumentMapper.countByStatus("READY"),
                documentChunkMapper.countSearchable()
        );
    }

    public List<KnowledgeBaseVO> listKnowledgeBases() {
        return knowledgeBaseMapper.selectAllActive().stream()
                .map(KnowledgeBaseVO::of)
                .toList();
    }

    public List<KnowledgeDocumentVO> listDocuments(Long knowledgeBaseId) {
        if (knowledgeBaseId == null) {
            throw new IllegalArgumentException("knowledgeBaseId不能为空");
        }
        return knowledgeDocumentMapper.selectByKnowledgeBaseId(knowledgeBaseId).stream()
                .map(KnowledgeDocumentVO::of)
                .toList();
    }

    public DocumentContentVO getDocumentContent(Long documentId) {
        if (documentId == null) {
            throw new IllegalArgumentException("documentId不能为空");
        }

        KnowledgeDocument document = knowledgeDocumentMapper.selectById(documentId);
        if (document == null || "DELETED".equals(document.getStatus())) {
            throw new IllegalArgumentException("文档不存在");
        }

        List<DocumentChunkVO> chunks = documentChunkMapper.selectByDocumentId(documentId).stream()
                .map(DocumentChunkVO::of)
                .toList();
        String content = chunks.stream()
                .map(DocumentChunkVO::content)
                .filter(text -> text != null && !text.isBlank())
                .reduce((left, right) -> left + "\n\n" + right)
                .orElse("");

        return new DocumentContentVO(KnowledgeDocumentVO.of(document), chunks, content);
    }

    @Transactional
    public void renameKnowledgeBase(Long knowledgeBaseId, String name) {
        if (knowledgeBaseId == null) {
            throw new IllegalArgumentException("knowledgeBaseId不能为空");
        }
        String nextName = name == null ? "" : name.trim();
        if (nextName.isBlank()) {
            throw new IllegalArgumentException("知识库名称不能为空");
        }
        if (knowledgeBaseMapper.updateName(knowledgeBaseId, nextName) == 0) {
            throw new IllegalArgumentException("知识库不存在");
        }
    }

    @Transactional
    public void deleteKnowledgeBase(Long knowledgeBaseId) {
        if (knowledgeBaseId == null) {
            throw new IllegalArgumentException("knowledgeBaseId不能为空");
        }
        if (knowledgeBaseMapper.markDeleted(knowledgeBaseId) == 0) {
            throw new IllegalArgumentException("知识库不存在");
        }
        knowledgeDocumentMapper.markDeletedByKnowledgeBaseId(knowledgeBaseId);
        documentChunkMapper.markDeletedByKnowledgeBaseId(knowledgeBaseId);
    }

    public OriginalDocumentResource getOriginalDocument(Long documentId) {
        if (documentId == null) {
            throw new IllegalArgumentException("documentId不能为空");
        }

        KnowledgeDocument document = knowledgeDocumentMapper.selectById(documentId);
        if (document == null || "DELETED".equals(document.getStatus())) {
            throw new IllegalArgumentException("文档不存在");
        }
        if (document.getStorageUrl() == null || document.getStorageUrl().isBlank()) {
            throw new IllegalArgumentException("文档没有原始文件路径");
        }

        Path uploadRoot = Paths.get("uploads").toAbsolutePath().normalize();
        Path filePath = Paths.get(document.getStorageUrl()).toAbsolutePath().normalize();
        if (!filePath.startsWith(uploadRoot)) {
            throw new IllegalArgumentException("文档路径不合法");
        }
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw new IllegalArgumentException("原始文件不存在");
        }

        return new OriginalDocumentResource(
                new FileSystemResource(filePath),
                document.getFileName(),
                document.getMimeType()
        );
    }

    public record IngestResult(Long documentId, int savedChunkCount, int indexedChunkCount) {
    }

    public record KnowledgeStats(int documentCount, int searchableDocumentCount, int chunkCount) {
    }

    public record KnowledgeBaseVO(
            Long id,
            String kbCode,
            String name,
            String description,
            String visibility,
            String status,
            String embeddingModel,
            String updatedAt
    ) {
        public static KnowledgeBaseVO of(KnowledgeBase knowledgeBase) {
            return new KnowledgeBaseVO(
                    knowledgeBase.getId(),
                    knowledgeBase.getKbCode(),
                    knowledgeBase.getName(),
                    knowledgeBase.getDescription(),
                    knowledgeBase.getVisibility(),
                    knowledgeBase.getStatus(),
                    knowledgeBase.getEmbeddingModel(),
                    knowledgeBase.getUpdatedAt() == null ? null : knowledgeBase.getUpdatedAt().toString()
            );
        }
    }

    public record KnowledgeDocumentVO(
            Long id,
            Long knowledgeBaseId,
            String title,
            String fileName,
            String fileType,
            String mimeType,
            Long fileSize,
            String sourceType,
            String status,
            String createdAt,
            String updatedAt
    ) {
        public static KnowledgeDocumentVO of(KnowledgeDocument document) {
            return new KnowledgeDocumentVO(
                    document.getId(),
                    document.getKnowledgeBaseId(),
                    document.getTitle(),
                    document.getFileName(),
                    document.getFileType(),
                    document.getMimeType(),
                    document.getFileSize(),
                    document.getSourceType(),
                    document.getStatus(),
                    document.getCreatedAt() == null ? null : document.getCreatedAt().toString(),
                    document.getUpdatedAt() == null ? null : document.getUpdatedAt().toString()
            );
        }
    }

    public record DocumentChunkVO(
            Long id,
            Integer chunkIndex,
            Integer pageNo,
            String sectionTitle,
            Integer tokenCount,
            String status,
            String content
    ) {
        public static DocumentChunkVO of(DocumentChunk chunk) {
            return new DocumentChunkVO(
                    chunk.getId(),
                    chunk.getChunkIndex(),
                    chunk.getPageNo(),
                    chunk.getSectionTitle(),
                    chunk.getTokenCount(),
                    chunk.getStatus(),
                    chunk.getContent()
            );
        }
    }

    public record DocumentContentVO(
            KnowledgeDocumentVO document,
            List<DocumentChunkVO> chunks,
            String content
    ) {
    }

    public record OriginalDocumentResource(
            org.springframework.core.io.Resource resource,
            String fileName,
            String mimeType
    ) {
    }


    //用于upload 检测文件是否合法
    private void validate(MultipartFile file) throws IOException{
        if (file.isEmpty()) {
            throw new IllegalArgumentException(
                    "上传文件为空");
        }

        String fileName = file.getOriginalFilename();

        //后缀名校验
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        //MIME TYPE校验
        //不使用getContentType获取，可以被修改
        String mimeType = tika.detect(file.getInputStream());
        Set<String> expectedMimeTypes = FILE_TYPE_MAPPING.get(FilenameUtils.getExtension(fileName).toLowerCase(Locale.ROOT));
        //检测mimeType是否与文件后缀对应
        if (expectedMimeTypes == null ||
                !expectedMimeTypes.contains(mimeType)) {
            throw new IllegalArgumentException("文件类型校验失败:"+mimeType+" 期望："+expectedMimeTypes);
        }
    }

}
