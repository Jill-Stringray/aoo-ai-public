package com.clockshore.aooai.service;

import com.clockshore.aooai.config.ChunkQualityProperties;
import dev.langchain4j.data.segment.TextSegment;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ChunkQualityFilter {

    private final ChunkQualityProperties properties;

    public List<Document> filterDocuments(List<Document> chunks) {
        return chunks.stream()
                .filter(document -> isHighQuality(document.getText()))
                .toList();
    }

    public List<TextSegment> filterSegments(List<TextSegment> segments) {
        return segments.stream()
                .filter(segment -> isHighQuality(segment.text()))
                .toList();
    }

    public boolean isHighQuality(String text) {
        if (text == null) {
            return false;
        }

        String trimmed = text.trim();
        if (trimmed.length() < properties.getMinTextLength()) {
            return false;
        }
        if (looksLikePageNumber(trimmed)) {
            return false;
        }
        if (newlineRatio(text) > properties.getMaxNewlineRatio()) {
            return false;
        }
        if (singleCharLineRatio(text) > properties.getMaxSingleCharLineRatio()) {
            return false;
        }
        if (splitWordLineRatio(text) > properties.getMaxSplitWordLineRatio()) {
            return false;
        }
        if (alphaNumericRatio(text) < properties.getMinAlphaNumericRatio()) {
            return false;
        }
        return !hasExcessiveRepeatedChars(text);
    }

    private boolean looksLikePageNumber(String text) {
        return text.matches("(?i)^(page\\s*)?\\d{1,4}$")
                || text.matches("^第\\s*\\d{1,4}\\s*页$");
    }

    private double newlineRatio(String text) {
        return ratio(text.chars().filter(ch -> ch == '\n').count(), text.length());
    }

    private double singleCharLineRatio(String text) {
        List<String> lines = nonBlankLines(text);
        if (lines.isEmpty()) {
            return 1.0;
        }
        long singleCharLines = lines.stream()
                .filter(line -> codePointCount(line) == 1)
                .count();
        return ratio(singleCharLines, lines.size());
    }

    private double splitWordLineRatio(String text) {
        List<String> lines = nonBlankLines(text);
        if (lines.size() < properties.getMinSplitWordLineCount()) {
            return 0.0;
        }
        long splitWordLines = lines.stream()
                .filter(this::looksLikeSplitWordPart)
                .count();
        return ratio(splitWordLines, lines.size());
    }

    private boolean looksLikeSplitWordPart(String line) {
        String compact = line.replaceAll("\\s+", "");
        return compact.length() <= 3 && compact.matches("[\\p{L}]+");
    }

    private double alphaNumericRatio(String text) {
        long nonWhitespace = 0;
        long alphaNumeric = 0;
        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            if (!Character.isWhitespace(codePoint)) {
                nonWhitespace++;
                if (Character.isLetterOrDigit(codePoint)) {
                    alphaNumeric++;
                }
            }
            i += Character.charCount(codePoint);
        }
        return ratio(alphaNumeric, nonWhitespace);
    }

    private boolean hasExcessiveRepeatedChars(String text) {
        Map<Integer, Integer> counts = new HashMap<>();
        int maxCount = 0;
        int maxRunLength = 0;
        int currentRunLength = 0;
        int previousCodePoint = -1;
        int nonWhitespace = 0;

        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            if (!Character.isWhitespace(codePoint)) {
                nonWhitespace++;
                int count = counts.merge(codePoint, 1, Integer::sum);
                maxCount = Math.max(maxCount, count);
                if (codePoint == previousCodePoint) {
                    currentRunLength++;
                } else {
                    currentRunLength = 1;
                    previousCodePoint = codePoint;
                }
                maxRunLength = Math.max(maxRunLength, currentRunLength);
            }
            i += Character.charCount(codePoint);
        }

        return maxRunLength > properties.getMaxRepeatedCharRunLength()
                || ratio(maxCount, nonWhitespace) > properties.getMaxRepeatedCharRatio();
    }

    private List<String> nonBlankLines(String text) {
        return text.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();
    }

    private int codePointCount(String text) {
        return text.codePointCount(0, text.length());
    }

    private double ratio(long numerator, long denominator) {
        if (denominator == 0) {
            return 0.0;
        }
        return (double) numerator / denominator;
    }
}
