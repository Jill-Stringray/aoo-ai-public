package com.clockshore.aooai.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TokenSplitterService {

    private final TokenTextSplitter splitter = new TokenTextSplitter();

    public List<Document> split(List<Document> docs) {
        return splitter.apply(docs);
    }
}
