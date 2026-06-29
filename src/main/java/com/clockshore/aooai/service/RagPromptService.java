package com.clockshore.aooai.service;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.web.service.invoker.HttpRequestValues;

import java.util.Map;

@Service
public class RagPromptService {

    public String buildAll(Document document){
        return buildPrompt(document.getText()) + buildMetadata(document);
    }

    public String buildPrompt(String context) {
        return """
                ===== 知识库内容 =====

                %s
                """
                .formatted(context);
    }

    public String buildMetadata(Document document) {

        Map<String, Object> metadata = document.getMetadata();

        return """

            来源：%s 第%s页

            """
                .formatted(
                        metadata.getOrDefault("fileName", "未知文档"),
                        metadata.getOrDefault("pageNo", "未知")
                );
    }
}