package com.clockshore.aooai.service;

import com.clockshore.aooai.domain.model.KnowledgeBase;

import java.util.List;

public interface KnowledgeBaseService {
    List<KnowledgeBase> list();

    KnowledgeBase create(String name, String description, String owner);
}
