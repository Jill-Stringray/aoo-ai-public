package com.clockshore.aooai;

import com.clockshore.aooai.entity.QueryRewriteResult;
import com.clockshore.aooai.service.KnowledgeRetrievalService;
import com.clockshore.aooai.service.QueryRewriteService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@SpringBootTest(classes = AooAiApplication.class)
class RetrievalDebugTest {

    private static final String QUESTION = "吉尔是谁";
    private static final int TOP_K = 5;
    private static final Long KNOWLEDGE_BASE_ID = null;

    @Resource
    private QueryRewriteService queryRewriteService;

    @Resource
    private KnowledgeRetrievalService knowledgeRetrievalService;

    @Test
    @DisplayName("Retrieval Debug")
    void showRetrievalDebugProcess() {
        List<String> rewriteQueries = rewrite(QUESTION);
        List<RecallResult> recallResults = retrieveAll(rewriteQueries);
        List<DedupResult> dedupResults = deduplicate(recallResults);

        System.out.println(buildDebugReport(QUESTION, rewriteQueries, recallResults, dedupResults));
    }

    private List<String> rewrite(String question) {
        QueryRewriteResult rewriteResult = queryRewriteService.rewrite(question);
        if (rewriteResult == null || rewriteResult.getQueries() == null || rewriteResult.getQueries().isEmpty()) {
            return List.of(question);
        }
        return rewriteResult.getQueries()
                .stream()
                .filter(query -> query != null && !query.isBlank())
                .toList();
    }

    private List<RecallResult> retrieveAll(List<String> rewriteQueries) {
        List<RecallResult> recallResults = new ArrayList<>();
        for (String query : rewriteQueries) {
            List<Document> documents = knowledgeRetrievalService.retrieve(query, TOP_K, KNOWLEDGE_BASE_ID);
            for (Document document : documents) {
                recallResults.add(RecallResult.from(query, document));
            }
        }
        return recallResults;
    }

    private List<DedupResult> deduplicate(List<RecallResult> recallResults) {
        return recallResults.stream()
                .collect(Collectors.groupingBy(
                        RecallResult::dedupKey,
                        LinkedHashMap::new,
                        Collectors.maxBy(Comparator.comparingDouble(RecallResult::score))
                ))
                .values()
                .stream()
                .flatMap(Optional::stream)
                .map(DedupResult::from)
                .sorted(Comparator.comparingDouble(DedupResult::finalScore).reversed())
                .toList();
    }

    private String buildDebugReport(
            String question,
            List<String> rewriteQueries,
            List<RecallResult> recallResults,
            List<DedupResult> dedupResults
    ) {
        StringBuilder report = new StringBuilder();
        report.append("用户问题:\n")
                .append(question)
                .append("\n\n");

        report.append("Rewrite Query:\n");
        for (int i = 0; i < rewriteQueries.size(); i++) {
            report.append(i + 1)
                    .append(". ")
                    .append(rewriteQueries.get(i))
                    .append("\n");
        }

        report.append("\n\n召回结果:\n\n")
                .append(String.format("%-24s %-30s %s%n", "chunk_id", "query", "score"))
                .append("----------------------------------------------------------------\n");
        for (RecallResult result : recallResults) {
            report.append(String.format("%-24s %-30s %.4f%n",
                    result.displayChunkId(),
                    result.query(),
                    result.score()));
        }

        report.append("\n\n去重后:\n\n")
                .append(String.format("%-24s %s%n", "chunk_id", "final_score"))
                .append("----------------------------------------\n");
        for (DedupResult result : dedupResults) {
            report.append(String.format("%-24s %.4f%n",
                    result.displayChunkId(),
                    result.finalScore()));
        }

        report.append("\n\n最终送入LLM:\n");
        for (DedupResult result : dedupResults) {
            report.append("chunk ")
                    .append(result.displayChunkId())
                    .append("\n");
        }

        return report.toString();
    }

    private record RecallResult(String dedupKey, Long chunkId, String query, Double score) {

        private static RecallResult from(String query, Document document) {
            Map<String, Object> metadata = document.getMetadata();
            Long chunkId = toLong(metadata.get("chunkId"));
            String dedupKey = buildDedupKey(document, chunkId);
            return new RecallResult(dedupKey, chunkId, query, document.getScore());
        }

        private static String buildDedupKey(Document document, Long chunkId) {
            if (chunkId != null) {
                return "chunkId:" + chunkId;
            }
            Object chunkKey = document.getMetadata().get("chunkKey");
            if (chunkKey != null && !chunkKey.toString().isBlank()) {
                return "chunkKey:" + chunkKey;
            }
            String text = document.getText();
            return "text:" + (text == null ? "" : text.replaceAll("\\s+", " ").trim());
        }

        private String displayChunkId() {
            return chunkId == null ? dedupKey : chunkId.toString();
        }
    }

    private record DedupResult(String dedupKey, Long chunkId, Double finalScore) {

        private static DedupResult from(RecallResult result) {
            return new DedupResult(result.dedupKey(), result.chunkId(), result.score());
        }

        private String displayChunkId() {
            return chunkId == null ? dedupKey : chunkId.toString();
        }
    }

    private static Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = value.toString();
        return text.isBlank() ? null : Long.valueOf(text);
    }
}
