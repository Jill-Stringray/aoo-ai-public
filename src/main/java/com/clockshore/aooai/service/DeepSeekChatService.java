package com.clockshore.aooai.service;

import com.clockshore.aooai.chat.ChatRequest;
import com.clockshore.aooai.entity.ConversationMessage;
import com.clockshore.aooai.mapper.ConversationMessageMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DeepSeekChatService {

    private final ChatClient chatClient;
    private ConversationMessageMapper conversationMessageMapper;

    public DeepSeekChatService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    private static final double DEFAULT_TEMPERATURE = 0.7D;


    public String chat(ChatRequest chatRequest) {

        //Advisor = ChatClient 的“拦截器 / 增强器 / 插件系统”

        return chatClient.prompt()
                .messages(buildMessages(chatRequest))
                .call()
                .content();
    }

    public List<Message> buildMessages(ChatRequest chatRequest){
        List<Message> messages = new ArrayList<>();
        messages.addAll(buildHistory(chatRequest));
        return messages;
    }

    public List<Message> buildHistory(ChatRequest chatRequest){
        //构建历史消息
        List<Message> history = new ArrayList<>();
        List<ConversationMessage> messagesList = conversationMessageMapper.selectByConversationId(chatRequest.getConversationId());
        for(ConversationMessage msg : messagesList){
            if ("user".equals(msg.getRole())) {
                history.add(new UserMessage(msg.getContent()));
            } else if ("assistant".equals(msg.getRole())) {
                history.add(new AssistantMessage(msg.getContent()));
            } else if ("system".equals(msg.getRole())) {
                history.add(new SystemMessage(msg.getContent()));
            }
        }
        return history;
    }



    /**

    public String streamChat(ChatRequest request, Consumer<String> onDelta) {
        if (!StringUtils.hasText(aiProperties.getApiKey())) {
            throw new IllegalStateException("请先在 application.yaml 中配置 deepseek.api-key");
        }
        String url = aiProperties.getBaseUrl() + "/chat/completions";
        Map<String, Object> payload = buildChatPayload(request, true);
        String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("序列化 DeepSeek 请求失败", e);
        }

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMinutes(15))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + aiProperties.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<InputStream> response = streamHttpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() / 100 != 2) {
                String err = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                throw new IllegalStateException("DeepSeek 流式请求失败: HTTP " + response.statusCode() + " " + err);
            }
            return readSseStream(response.body(), onDelta);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("DeepSeek 流式请求异常", e);
        }
    }

    private String readSseStream(InputStream body, Consumer<String> onDelta) throws Exception {
        StringBuilder full = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) {
                    continue;
                }
                String data = line.substring(5).trim();
                if (data.isEmpty()) {
                    continue;
                }
                if ("[DONE]".equals(data)) {
                    break;
                }
                JsonNode root = objectMapper.readTree(data);
                JsonNode choices = root.path("choices");
                if (!choices.isArray() || choices.isEmpty()) {
                    continue;
                }
                String piece = choices.get(0).path("delta").path("content").asText("");
                if (StringUtils.hasText(piece)) {
                    full.append(piece);
                    onDelta.accept(piece);
                }
            }
        }
        return full.toString();
    }

    private Map<String, Object> buildChatPayload(ChatRequest request, boolean streamingMode) {
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("model", resolveModel(request));
        payload.put("messages", resolveMessages(request));
        payload.put("temperature", resolveTemperature(request));
        if (streamingMode) {
            payload.put("stream", true);
        } else if (request.getStream() != null) {
            payload.put("stream", request.getStream());
        }
        return payload;
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
            for (ChatMessage message : request.getMessages()) {
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
    }*/
}
