package com.clockshore.aooai.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Setter
@Getter
@Service
@ConfigurationProperties(prefix = "app.rag")
public class RagService {

    private boolean enabled = false;
    private int topK = 3;
    private List<KnowledgeDoc> docs = new ArrayList<>();

    public String buildContext(String query) {
        if (!enabled || !StringUtils.hasText(query) || docs == null || docs.isEmpty()) {
            return "";
        }

        Set<String> queryTokens = tokenize(query);
        if (queryTokens.isEmpty()) {
            return "";
        }

        List<KnowledgeDoc> selected = docs.stream()
                .filter(doc -> StringUtils.hasText(doc.getContent()))
                .map(doc -> new ScoredDoc(doc, overlapScore(queryTokens, doc.getContent())))
                .filter(scored -> scored.score > 0)
                .sorted(Comparator.comparingInt(ScoredDoc::score).reversed())
                .limit(Math.max(topK, 1))
                .map(scored -> scored.doc)
                .toList();

        if (selected.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder("以下是可参考的知识库片段，仅在相关时使用：\n");
        for (int i = 0; i < selected.size(); i++) {
            KnowledgeDoc doc = selected.get(i);
            String title = StringUtils.hasText(doc.getTitle()) ? doc.getTitle().trim() : "文档" + (i + 1);
            context.append(i + 1)
                    .append(". [")
                    .append(title)
                    .append("] ")
                    .append(doc.getContent().trim())
                    .append("\n");
        }
        return context.toString().trim();
    }

    private int overlapScore(Set<String> queryTokens, String text) {
        Set<String> docTokens = tokenize(text);
        int score = 0;
        for (String token : queryTokens) {
            if (docTokens.contains(token)) {
                score++;
            }
        }
        return score;
    }

    private Set<String> tokenize(String text) {
        if (!StringUtils.hasText(text)) {
            return Set.of();
        }
        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+"))
                .filter(token -> token.length() >= 2)
                .collect(Collectors.toCollection(HashSet::new));
    }

    @Getter
    @Setter
    public static class KnowledgeDoc {
        private String title;
        private String content;
    }

    private record ScoredDoc(KnowledgeDoc doc, int score) {
    }
}