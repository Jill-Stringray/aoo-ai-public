package com.clockshore.aooai.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SystemConfigService {

    private volatile SystemConfig config;

    public SystemConfigService(
            @Value("${spring.ai.openai.embedding.options.model:BAAI/bge-m3}") String embeddingModel,
            @Value("${spring.ai.openai.chat.options.model:deepseek-chat}") String llmModel) {
        this.config = new SystemConfig(
                embeddingModel,
                llmModel,
                800,
                80,
                5,
                0.41,
                true
        );
    }

    public SystemConfig getConfig() {
        return config;
    }

    public SystemConfig updateConfig(SystemConfig request) {
        if (request == null) {
            throw new IllegalArgumentException("系统配置不能为空");
        }
        config = new SystemConfig(
                requireText(request.embeddingModel(), "Embedding模型不能为空"),
                requireText(request.llmModel(), "LLM模型不能为空"),
                requirePositive(request.chunkSize(), "Chunk大小必须大于0"),
                requireNonNegative(request.chunkOverlap(), "Chunk overlap不能小于0"),
                requirePositive(request.topK(), "TopK召回数量必须大于0"),
                requireThreshold(request.similarityThreshold()),
                request.rerankEnabled()
        );
        return config;
    }

    private String requireText(String value, String message) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return trimmed;
    }

    private Integer requirePositive(Integer value, String message) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private Integer requireNonNegative(Integer value, String message) {
        if (value == null || value < 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private Double requireThreshold(Double value) {
        if (value == null || value < 0 || value > 1) {
            throw new IllegalArgumentException("相似度阈值必须在0到1之间");
        }
        return value;
    }

    public record SystemConfig(
            String embeddingModel,
            String llmModel,
            Integer chunkSize,
            Integer chunkOverlap,
            Integer topK,
            Double similarityThreshold,
            Boolean rerankEnabled
    ) {
    }
}
