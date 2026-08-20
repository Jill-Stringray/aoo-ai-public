package com.clockshore.aooai.service;

import com.clockshore.aooai.Utility.JsonUtil;
import com.clockshore.aooai.config.DoclingServiceProperties;
import com.clockshore.aooai.entity.KnowledgeDocument;
import com.clockshore.aooai.mapper.KnowledgeDocumentMapper;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DocumentReaderService {

    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final DoclingServiceProperties doclingServiceProperties;
    private final WebClient doclingServiceWebClient;
    private final JsonUtil jsonUtil;

    public DocumentReaderService(
            KnowledgeDocumentMapper knowledgeDocumentMapper,
            DoclingServiceProperties doclingServiceProperties,
            @Qualifier("doclingService") WebClient doclingServiceWebClient,
            JsonUtil jsonUtil
    ) {
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
        this.doclingServiceProperties = doclingServiceProperties;
        this.doclingServiceWebClient = doclingServiceWebClient;
        this.jsonUtil = jsonUtil;
    }

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
        if (resource instanceof FileSystemResource fileSystemResource
                && shouldUseDocling(fileName)) {
            try {
                return readWithDocling(fileName, fileSystemResource);
            } catch (Exception ex) {
                if (!doclingServiceProperties.isFallbackToLocalReader()) {
                    throw ex;
                }
                log.warn("Docling服务解析失败，将回退本地解析: fileName={}", fileName, ex);
            }
        }

        if (fileName.endsWith(".txt") || fileName.endsWith(".md")) {
            TextReader reader = new TextReader(resource);
            reader.setCharset(StandardCharsets.UTF_8);
            return reader.get();
        } else if (fileName.endsWith(".pdf")) {
            return readPdfDocument(resource);
        }else if(fileName.endsWith(".docx") || fileName.endsWith(".doc")){
            TikaDocumentReader reader = new TikaDocumentReader(resource);
            return reader.get();
        }
        throw new IllegalArgumentException(
                "暂不支持的文件类型: " + fileName
        );
    }

    private boolean shouldUseDocling(String fileName) {
        if (!doclingServiceProperties.isEnabled()) {
            return false;
        }
        String extension = getExtension(fileName);
        return doclingServiceProperties.getSupportedExtensions().stream()
                .anyMatch(extension::equalsIgnoreCase);
    }

    private List<Document> readWithDocling(String fileName, FileSystemResource resource) {
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("file", resource)
                .filename(resource.getFilename() == null ? fileName : resource.getFilename());

        DoclingConvertResponse response = doclingServiceWebClient.post()
                .uri("/convert")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .retrieve()
                .bodyToMono(DoclingConvertResponse.class)
                .block(doclingServiceProperties.getTimeout());

        if (response == null) {
            throw new IllegalStateException("Docling服务未返回有效内容");
        }

        List<Document> outlineDocuments = convertOutlineToDocuments(fileName, response);
        if (!outlineDocuments.isEmpty()) {
            return outlineDocuments;
        }

        if (response.markdown() == null || response.markdown().isBlank()) {
            throw new IllegalStateException("Docling服务未返回有效Markdown内容");
        }
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "docling");
        metadata.put("fileName", fileName);
        Document document = new Document(response.markdown(), metadata);
        return List.of(document);
    }

    public List<Document> readDoclingOutline(Path outlineJsonPath, String fileName) throws IOException {
        String outlineJson = Files.readString(outlineJsonPath, StandardCharsets.UTF_8);
        DoclingConvertResponse response = jsonUtil.fromJson(
                outlineJson,
                new TypeReference<>() {}
        );
        return convertOutlineToDocuments(fileName, response);
    }

    private List<Document> convertOutlineToDocuments(String fileName, DoclingConvertResponse response) {
        List<Document> documents = new ArrayList<>();
        if (response.outline() != null && !response.outline().isEmpty()) {
            for (DoclingOutlineNode node : response.outline()) {
                // 每个有正文的小节独立成为一个 Document，让后续切片继承 PDF 目录路径。
                collectOutlineDocuments(fileName, node, new ArrayList<>(), documents);
            }
        }
        if (response.unassignedContentPages() != null) {
            for (DoclingContentPage page : response.unassignedContentPages()) {
                if (page.markdown() == null || page.markdown().isBlank()) {
                    continue;
                }
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("source", "docling");
                metadata.put("fileName", fileName);
                metadata.put("sectionTitle", "未归属内容");
                List<String> sectionPath = List.of("未归属内容");
                metadata.put("sectionPath", sectionPath);
                metadata.put("page_number", page.pageNo());
                metadata.put("pageNo", page.pageNo());
                documents.add(new Document(page.markdown(), metadata));
            }
        }
        return documents;
    }

    private void collectOutlineDocuments(
            String fileName,
            DoclingOutlineNode node,
            List<String> parentPath,
            List<Document> documents) {

        List<String> currentPath = new ArrayList<>(parentPath);
        if (node.title() != null && !node.title().isBlank()) {
            currentPath.add(node.title().trim());
        }

        if (node.content() != null && !node.content().isBlank()) {
            List<String> sectionPath = resolveSectionPath(node, currentPath);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", "docling");
            metadata.put("fileName", fileName);
            metadata.put("sectionTitle", node.title());
            metadata.put("sectionPath", sectionPath);
            metadata.put("outlineLevel", node.level());
            metadata.put("page_number", node.pageNo());
            metadata.put("pageNo", node.pageNo());
            metadata.put("contentPages", resolveContentPages(node.contentPages()));

            documents.add(new Document(node.content(), metadata));
        }

        if (node.children() != null) {
            for (DoclingOutlineNode child : node.children()) {
                collectOutlineDocuments(fileName, child, currentPath, documents);
            }
        }
    }

    private List<String> resolveSectionPath(DoclingOutlineNode node, List<String> currentPath) {
        // 新版 Docling 会直接返回 section_path；旧版 outline.json 没有时用递归路径兜底。
        if (node.sectionPath() != null && !node.sectionPath().isEmpty()) {
            return node.sectionPath().stream()
                    .filter(text -> text != null && !text.isBlank())
                    .toList();
        }
        return currentPath.stream()
                .filter(text -> text != null && !text.isBlank())
                .toList();
    }

    private String resolveContentPages(List<DoclingContentPage> contentPages) {
        if (contentPages == null || contentPages.isEmpty()) {
            return "";
        }
        return contentPages.stream()
                .map(DoclingContentPage::pageNo)
                .distinct()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private String getExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private record DoclingConvertResponse(
            String markdown,
            List<DoclingOutlineNode> outline,
            @JsonProperty("unassigned_content_pages")
            List<DoclingContentPage> unassignedContentPages
    ) {
    }

    private record DoclingOutlineNode(
            String title,
            Integer level,
            @JsonProperty("page_no")
            Integer pageNo,
            @JsonProperty("section_path")
            List<String> sectionPath,
            List<DoclingOutlineNode> children,
            @JsonProperty("content_pages")
            List<DoclingContentPage> contentPages,
            String content
    ) {
    }

    private record DoclingContentPage(
            @JsonProperty("page_no")
            Integer pageNo,
            String markdown
    ) {
    }

    private List<Document> readPdfDocument(Resource resource) {
        PagePdfDocumentReader reader = new PagePdfDocumentReader(resource);
        List<Document> docs = reader.get();
        int totalPages = docs.stream()
                .mapToInt(this::resolvePdfEndPageNumber)
                .max()
                .orElse(0);

        for (Document doc : docs) {
            int pageNumber = resolvePdfStartPageNumber(doc);
            doc.getMetadata().put("page_number", pageNumber);
            doc.getMetadata().put("pageNo", pageNumber);
            doc.getMetadata().put("total_pages", totalPages);
            doc.getMetadata().put("page_count", totalPages);
        }

        return docs;
    }

    private int resolvePdfStartPageNumber(Document doc) {
        return toInt(doc.getMetadata().get(PagePdfDocumentReader.METADATA_START_PAGE_NUMBER), 1);
    }

    private int resolvePdfEndPageNumber(Document doc) {
        Object endPageNumber = doc.getMetadata().get(PagePdfDocumentReader.METADATA_END_PAGE_NUMBER);
        if (endPageNumber != null) {
            return toInt(endPageNumber, 1);
        }
        return resolvePdfStartPageNumber(doc);
    }

    private int toInt(Object value, int defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
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

