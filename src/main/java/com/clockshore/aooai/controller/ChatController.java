package com.clockshore.aooai.controller;

import com.clockshore.aooai.common.ApiResponse;
import com.clockshore.aooai.dto.message.ChatRequest;
import com.clockshore.aooai.enumType.ResultCode;
import com.clockshore.aooai.service.ChatService;
import com.clockshore.aooai.service.KnowledgeService;
import com.clockshore.aooai.service.SseService;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    @Resource
    private SseService sseService;

    @Resource
    private ChatService chatService;

    @Resource
    private KnowledgeService knowledgeService;

    /**
     * @param request
     * @return
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestBody ChatRequest request) {
        return sseService.stream(chatService.streamChat(request));
    }

}
