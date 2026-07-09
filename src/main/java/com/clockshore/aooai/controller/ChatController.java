package com.clockshore.aooai.controller;

import com.clockshore.aooai.common.ApiResponse;
import com.clockshore.aooai.dto.message.ChatRequest;
import com.clockshore.aooai.dto.message.ChatRequestVO;
import com.clockshore.aooai.dto.message.ConversationVO;
import com.clockshore.aooai.enumType.ResultCode;
import com.clockshore.aooai.security.SecurityUtils;
import com.clockshore.aooai.service.ChatService;
import com.clockshore.aooai.service.ConversationService;
import com.clockshore.aooai.service.KnowledgeService;
import com.clockshore.aooai.service.SseService;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    @Resource
    private SseService sseService;

    @Resource
    private ChatService chatService;

    @Resource
    private KnowledgeService knowledgeService;

    @Resource
    private ConversationService conversationService;


    /**
     * @param request
     * @return
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestBody ChatRequest request) {
        return sseService.stream(chatService.streamChat(request));
    }

    @GetMapping(value = "/conversation/list")
    public ApiResponse<List<ConversationVO>> ConversationList(){
        return ApiResponse.success(conversationService.fetchConversationList(SecurityUtils.getUserId()));
    }

    //todo
    @GetMapping()
    public ApiResponse<List<Message>> conversationMessage(@RequestParam("ConversationId")Long conversationId){

    }

}
