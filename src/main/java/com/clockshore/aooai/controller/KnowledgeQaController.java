package com.clockshore.aooai.controller;

import com.clockshore.aooai.dto.AskRequest;
import com.clockshore.aooai.dto.AskResponse;
import com.clockshore.aooai.service.KnowledgeQaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/qa")
@RequiredArgsConstructor
@Validated
public class KnowledgeQaController {

    private final KnowledgeQaService knowledgeQaService;

    @PostMapping("/ask")
    public AskResponse ask(@Valid @RequestBody AskRequest request) {
        return knowledgeQaService.ask(request);
    }

    @PostMapping(value = "/ask-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> askStream(@Valid @RequestBody AskRequest request) {
        return knowledgeQaService.askStream(request);
    }
}
