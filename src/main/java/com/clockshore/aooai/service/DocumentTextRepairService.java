package com.clockshore.aooai.service;

import com.clockshore.aooai.config.DocumentTextRepairProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentTextRepairService {

    private static final String UNICODE_HYPHENS = "[\\u2010\\u2011\\u2012\\u2013\\u2014\\u2212]";

    private final DocumentTextRepairProperties properties;

    public List<Document> repair(List<Document> documents) {
        return documents.stream()
                .map(this::repair)
                .toList();
    }

    public Document repair(Document document) {
        if (document == null || document.getText() == null) {
            return document;
        }
        return new Document(repairText(document.getText()), document.getMetadata());
    }

    public String repairText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String repaired = text;
        if (properties.isRemoveSoftHyphen()) {
            repaired = repaired.replace("\u00AD", "");
        }
        if (properties.isNormalizeUnicodeHyphens()) {
            repaired = repaired.replaceAll(UNICODE_HYPHENS, "-");
        }
        if (properties.isJoinHyphenatedLineBreaks()) {
            repaired = repaired.replaceAll("(?U)(?<=\\p{L})-\\s*\\R\\s*(?=\\p{Ll})", "");
        }
        return repaired;
    }
}
