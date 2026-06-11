package com.clockshore.aooai.controller;

import com.clockshore.aooai.service.KnowledgeService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;


public class KnowledgeController {

    @Resource
    private KnowledgeService knowledgeService;

    @PostMapping("/knowledge/upload")
    public String upload(@RequestParam("file") MultipartFile file) {
        knowledgeService.upload(file);
        return "success";
    }
}
