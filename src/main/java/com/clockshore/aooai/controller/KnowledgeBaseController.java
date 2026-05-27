package com.clockshore.aooai.controller;

import com.clockshore.aooai.domain.model.KnowledgeBase;
import com.clockshore.aooai.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/knowledge-bases")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    @GetMapping
    public List<KnowledgeBase> list() {
        return knowledgeBaseService.list();
    }

    @PostMapping
    public KnowledgeBase create(@RequestBody Map<String, String> payload) {
        return knowledgeBaseService.create(payload.get("name"), payload.get("description"), payload.getOrDefault("owner", "student"));
    }
}
