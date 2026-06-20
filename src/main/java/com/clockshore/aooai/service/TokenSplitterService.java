package com.clockshore.aooai.service;

import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TokenSplitterService {

    private final DocumentSplitter splitter =
            DocumentSplitters.recursive(
                    500,
                    100
            );

    public List<TextSegment> split(List<Document> docs) {

        List<TextSegment> segments = new ArrayList<>();

        for (Document doc : docs) {
            segments.addAll(
                    splitter.split(convert(doc))
            );
        }

        return segments;
    }

    //链路原因，要将spring ai doc 转化成langchain4j doc
    private dev.langchain4j.data.document.Document convert(
            org.springframework.ai.document.Document doc) {

        Metadata metadata = new Metadata();

        doc.getMetadata().forEach(
                (k, v) -> metadata.put(k, String.valueOf(v))
        );

        return dev.langchain4j.data.document.Document.from(
                doc.getText(),
                metadata
        );
    }
}
