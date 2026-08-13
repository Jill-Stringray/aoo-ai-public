package com.clockshore.aooai.service;

import com.clockshore.aooai.Utility.JsonUtil;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.segment.TextSegment;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
public class TokenSplitterService {

    private final SystemConfigService systemConfigService;
    private final JsonUtil jsonUtil;

    public TokenSplitterService(SystemConfigService systemConfigService, JsonUtil jsonUtil) {
        this.systemConfigService = systemConfigService;
        this.jsonUtil = jsonUtil;
    }

    public List<TextSegment> split(List<Document> docs) {

        List<TextSegment> segments = new ArrayList<>();
        SystemConfigService.SystemConfig config = systemConfigService.getConfig();
        DocumentSplitter splitter = new DocumentByParagraphSplitter(
                config.chunkSize(),
                config.chunkOverlap()
        );

        for (Document doc : docs) {
            segments.addAll(
                    splitter.split(StoLConvert(doc))
            );
        }

        return segments;
    }

    //链路原因，要将spring ai doc 转化成langchain4j doc
    private dev.langchain4j.data.document.Document StoLConvert(
            org.springframework.ai.document.Document doc) {

        Metadata metadata = new Metadata();

        doc.getMetadata().forEach(
                (k, v) -> metadata.put(k, toMetadataString(v))
        );

        return dev.langchain4j.data.document.Document.from(
                doc.getText(),
                metadata
        );
    }

    private String toMetadataString(Object value) {
        // LangChain4j Metadata 只收字符串，结构化路径先转 JSON，保存 chunk 前再还原。
        if (value instanceof Collection<?> || value instanceof Map<?, ?>) {
            return jsonUtil.toJson(value);
        }
        return String.valueOf(value);
    }

}
