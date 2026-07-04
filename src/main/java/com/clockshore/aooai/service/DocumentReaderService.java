package com.clockshore.aooai.service;

import com.clockshore.aooai.entity.KnowledgeDocument;
import com.clockshore.aooai.mapper.KnowledgeDocumentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class DocumentReaderService {

    private KnowledgeDocumentMapper knowledgeDocumentMapper;
    //reader对文件进行解析成为document

    /**
     * 按路径解析
     */
    public List<Document> read(Path filePath){
        //统一化
        String fileName = filePath.getFileName().toString().toLowerCase();
        //spring风格 resource
        Resource resource = new FileSystemResource(filePath.toFile());
        return readDocument(fileName, resource);
    }

    /**
     * 按DocumentId解析
     *
     */
    public List<Document> read(Long documentId){
        //统一化
        KnowledgeDocument knowledgeDocument = knowledgeDocumentMapper.selectById(documentId);
        String fileName = knowledgeDocument.getFileName().toLowerCase();
        //spring风格 resource
        Resource resource = new FileSystemResource(knowledgeDocument.getStorageUrl());
        return readDocument(fileName, resource);
    }

    private List<Document> readDocument(String fileName, Resource resource) {
        if (fileName.endsWith(".txt") || fileName.endsWith(".md")) {
            TextReader reader = new TextReader(resource);
            return reader.get();
        } else if (fileName.endsWith(".pdf")) {
            //tika本身支持pdf格式解析，但是不保留
            //pdf页码
            //分页信息
            //PDF元数据
            //所以使用PagePdfDocumentReader
            PagePdfDocumentReader reader = new PagePdfDocumentReader(resource);
            return reader.get();
        }else if(fileName.endsWith(".docx") || fileName.endsWith(".doc")){
            TikaDocumentReader reader = new TikaDocumentReader(resource);
            return reader.get();
        }
        throw new IllegalArgumentException(
                "暂不支持的文件类型: " + fileName
        );
    }

    public List<Document> enrichMetadata(
            List<Document> docs,
            KnowledgeDocument document) {

        for (Document doc : docs) {

            doc.getMetadata().put(
                    "knowledgeBaseId",
                    document.getKnowledgeBaseId()
            );

            doc.getMetadata().put(
                    "documentId",
                    document.getId()
            );

            doc.getMetadata().put(
                    "documentKey",
                    document.getDocumentKey()
            );

            doc.getMetadata().put(
                    "fileName",
                    document.getFileName()
            );
        }

        return docs;
    }

    public List<Document> normalize(List<Document> rawDocument) {
        List<Document> normalizedDocument = new ArrayList<>();

        for(Document raw : rawDocument){
            if(raw.getText() == null){
                continue;
            }

            String cleanText = raw.getText()
                    .replace("\u00A0", " ")
                    .replaceAll("[ \\t]+", " ")
                    .replaceAll("\\n{3,}", "\n\n")
                    .trim();

            if (cleanText.isBlank()) {
                continue;
            }

            Document newDoc = new Document(cleanText,raw.getMetadata());
            normalizedDocument.add(newDoc);
        }


        return normalizedDocument;

    }

}

