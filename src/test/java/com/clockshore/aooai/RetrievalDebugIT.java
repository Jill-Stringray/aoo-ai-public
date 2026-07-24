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

@SpringBootTest(classes = AooAiApplication.class)
class RetrievalDebugIT {

    private static final String QUESTION = "吉尔是谁";
    private static final int TOP_K = 5;
    private static final int RRF_K = 60;
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
            for (int i = 0; i < documents.size(); i++) {
                recallResults.add(RecallResult.from(query, i + 1, documents.get(i)));
            }
        }
        return recallResults;
    }

    private List<DedupResult> deduplicate(List<RecallResult> recallResults) {
        Map<String, DedupResult> dedupResultMap = new LinkedHashMap<>();
        for (RecallResult result : recallResults) {
            DedupResult old = dedupResultMap.get(result.dedupKey());
            double rrfScore = calculateRrfScore(result.rank());
            if (old == null) {
                dedupResultMap.put(result.dedupKey(), DedupResult.from(result, rrfScore));
            } else {
                dedupResultMap.put(result.dedupKey(), old.merge(result, rrfScore));
            }
        }

        return dedupResultMap.values()
                .stream()
                .sorted(Comparator.comparingDouble(DedupResult::finalScore).reversed())
                .toList();
    }

    private double calculateRrfScore(int rank) {
        return 1.0 / (RRF_K + rank);
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
                    .append(result.content)
                    .append("\n");
        }


        return report.toString();
    }

    private record RecallResult(String dedupKey, Long chunkId, String query, int rank, Double score, String content) {

        private static RecallResult from(String query, int rank, Document document) {
            Map<String, Object> metadata = document.getMetadata();
            Long chunkId = toLong(metadata.get("chunkId"));
            String dedupKey = buildDedupKey(document, chunkId);
            String content = document.getText();
            return new RecallResult(dedupKey, chunkId, query, rank, document.getScore(), content);
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

    private record DedupResult(String dedupKey, Long chunkId, Double sourceScore, Double finalScore, String content) {

        private static DedupResult from(RecallResult result, double rrfScore) {
            return new DedupResult(result.dedupKey(), result.chunkId(), result.score(), rrfScore, result.content());
        }

        private DedupResult merge(RecallResult result, double rrfScore) {
            if (isHigherScore(result.score(), sourceScore)) {
                return new DedupResult(dedupKey, result.chunkId(), result.score(), finalScore + rrfScore, result.content());
            }
            return new DedupResult(dedupKey, chunkId, sourceScore, finalScore + rrfScore, content);
        }

        private String displayChunkId() {
            return chunkId == null ? dedupKey : chunkId.toString();
        }
    }

    private static boolean isHigherScore(Double score, Double oldScore) {
        if (score == null) {
            return false;
        }
        if (oldScore == null) {
            return true;
        }
        return Double.compare(score, oldScore) > 0;
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
