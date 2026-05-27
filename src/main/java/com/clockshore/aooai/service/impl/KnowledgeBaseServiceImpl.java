package com.clockshore.aooai.service.impl;

import com.clockshore.aooai.domain.model.KnowledgeBase;
import com.clockshore.aooai.service.KnowledgeBaseService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {
    private final AtomicLong idGen = new AtomicLong(1);
    private final List<KnowledgeBase> data = new ArrayList<>();

    @Override
    public List<KnowledgeBase> list() {
        return data;
    }

    @Override
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
