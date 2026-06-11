package com.clockshore.aooai.service;

import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class KnowledgeService {

    @Resource
    private TokenSplitterService tokenSplitterService;

    private final VectorStore vectorStore;

    //此函数只进行导入＆保存，不处理
    public void upload(MultipartFile file) throws IOException {
        Path uploadDir = Paths.get("uploads");

        //若uploads不存在，创建文件夹
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        String originalName = file.getOriginalFilename();

        if (originalName == null || originalName.isBlank()) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        //UUID加原文件名的名字
        Path filePath = uploadDir.resolve(UUID.randomUUID() + "_" + originalName);

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

    }
}