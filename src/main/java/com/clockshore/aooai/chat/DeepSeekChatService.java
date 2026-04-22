package com.clockshore.aooai.chat;

import com.clockshore.aooai.config.AiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Service
public class DeepSeekChatService {

    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public DeepSeekChatService(AiProperties aiProperties, ObjectMapper objectMapper) {
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    public String chat(String message) {
        if (!StringUtils.hasText(aiProperties.getApiKey())) {
            throw new IllegalStateException("请先在 application.properties 中配置 deepseek.api-key");
        }

        String url = aiProperties.getBaseUrl() + "/chat/completions";

        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("model", aiProperties.getModel());
        payload.put("messages", Arrays.asList(buildUserMessage(message)));
        payload.put("temperature", 0.7);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(aiProperties.getApiKey());

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<Map<String, Object>>(payload, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

        return parseReply(response.getBody());
    }

    private Map<String, String> buildUserMessage(String message) {
        Map<String, String> userMessage = new HashMap<String, String>();
        userMessage.put("role", "user");
        userMessage.put("content", message);
        return userMessage;
    }

    private String parseReply(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode()) {
                throw new IllegalStateException("DeepSeek 返回格式异常: " + body);
            }
            return content.asText();
        } catch (Exception e) {
            throw new IllegalStateException("解析 DeepSeek 返回结果失败", e);
        }
    }
}
