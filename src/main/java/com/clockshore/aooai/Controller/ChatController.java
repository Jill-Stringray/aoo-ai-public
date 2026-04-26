package com.clockshore.aooai.Controller;

import com.clockshore.aooai.chat.ChatRequest;
import com.clockshore.aooai.chat.ChatResponse;
import com.clockshore.aooai.Service.DeepSeekChatService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("")
public class ChatController {

    private final DeepSeekChatService deepSeekChatService;

    public ChatController(DeepSeekChatService deepSeekChatService) {
        this.deepSeekChatService = deepSeekChatService;
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        if (request == null || !StringUtils.hasText(request.resolveMessage()) && (request.getMessages() == null || request.getMessages().isEmpty())) {
            return ResponseEntity.badRequest().body(new ChatResponse(null,"请输入消息，或提供符合 DeepSeek 格式的 messages 数组"));
        }

        try {
            String reply = deepSeekChatService.chat(request);
            return ResponseEntity.ok(new ChatResponse(reply,"请求正常完成"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ChatResponse(null,"发生错误：" +e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ChatResponse(null,"调用 DeepSeek 失败：" + e.getMessage()));
        }
    }
}
