package com.clockshore.aooai.service;

import com.clockshore.aooai.entity.KnowledgeDocument;
import com.clockshore.aooai.mapper.KnowledgeDocumentMapper;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
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

    Tika tika = new Tika();

    private final VectorStore vectorStore;

    private static final Map<String, Set<String>> FILE_TYPE_MAPPING = Map.of(
            "pdf", Set.of("application/pdf"),
            "doc", Set.of("application/msword"),
            "docx", Set.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            "txt", Set.of("text/plain"),
            "md", Set.of("text/plain", "text/markdown")
    );


    //此函数只进行导入＆保存文件，不处理
    //保存后写入mysql中的knowledge_document表来记录
    @Transactional
    public Path upload(MultipartFile file) throws IOException {
        validate(file);
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

            knowledgeDocumentMapper.insert(KnowledgeDocument.builder()
                    .knowledgeBaseId(1L)
                    .documentKey(u)
                    .fileName(originalName)
                    .fileType(extension)
                    .mimeType(tika.detect(file.getInputStream()))
                    .fileSize(file.getSize())
                    .storageUrl(filePath.toString())
                    .sourceType("UPLOAD")
                    .status("UPLOADED")
                    .build());
        } catch (Exception ex) {
            Files.deleteIfExists(filePath);
            throw ex;
        }
        return filePath;
    }


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
        Set<String> expectedMimeTypes = FILE_TYPE_MAPPING.get(FilenameUtils.getExtension(fileName));
        //检测mimeType是否与文件后缀对应
        if (expectedMimeTypes == null ||
                !expectedMimeTypes.contains(mimeType)) {
            throw new IllegalArgumentException("文件类型校验失败");
        }
    }

}