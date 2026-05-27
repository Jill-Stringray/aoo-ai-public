package com.clockshore.aooai.service;

import com.clockshore.aooai.domain.model.KnowledgeBase;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class KnowledgeBaseService {
    private final AtomicLong idGen = new AtomicLong(1);
    private final List<KnowledgeBase> data = new ArrayList<>();

    public List<KnowledgeBase> list() {
        return data;
    }

    public KnowledgeBase create(String name, String description, String owner) {
        KnowledgeBase kb = KnowledgeBase.builder()
                .id(idGen.getAndIncrement())
                .name(name)
                .description(description)
                .owner(owner)
                .build();
        data.add(kb);
        return kb;
    }
}
