package com.clockshore.aooai.chat;

import com.clockshore.aooai.config.AiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DeepSeekChatService {

    private static final double DEFAULT_TEMPERATURE = 0.7D;

    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public DeepSeekChatService(AiProperties aiProperties, ObjectMapper objectMapper) {
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    public String chat(ChatRequest request) {
        if (!StringUtils.hasText(aiProperties.getApiKey())) {
            throw new IllegalStateException("请先在 application.yaml 中配置 deepseek.api-key");
        }

        String url = aiProperties.getBaseUrl() + "/chat/completions";

        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("model", resolveModel(request));
        payload.put("messages", resolveMessages(request));
        payload.put("temperature", resolveTemperature(request));

        if (request.getStream() != null) {
            payload.put("stream", request.getStream());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(aiProperties.getApiKey());

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<Map<String, Object>>(payload, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

        return parseReply(response.getBody());
    }

    private String resolveModel(ChatRequest request) {
        if (StringUtils.hasText(request.getModel())) {
            return request.getModel().trim();
        }
        return aiProperties.getModel();
    }

    private double resolveTemperature(ChatRequest request) {
        if (request.getTemperature() != null) {
            return request.getTemperature();
        }
        return DEFAULT_TEMPERATURE;
    }

    private List<Map<String, String>> resolveMessages(ChatRequest request) {
        if (!CollectionUtils.isEmpty(request.getMessages())) {
            List<Map<String, String>> messages = new ArrayList<Map<String, String>>();
            for (ChatRequest.Message message : request.getMessages()) {
                if (message == null || !StringUtils.hasText(message.getContent())) {
                    continue;
                }
                messages.add(buildMessage(message.getRole(), message.getContent()));
            }
            if (!messages.isEmpty()) {
                return messages;
            }
        }

        String text = request.resolveMessage();
        if (!StringUtils.hasText(text)) {
            throw new IllegalArgumentException("请求体缺少 message 或 messages[].content 字段");
        }
        return List.of(buildMessage("user", text));
    }

    private Map<String, String> buildMessage(String role, String content) {
        Map<String, String> message = new HashMap<String, String>();
        message.put("role", StringUtils.hasText(role) ? role.trim() : "user");
        message.put("content", content.trim());
        return message;
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
