package com.clockshore.aooai.dto.rerank;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RerankResult(
        Integer index,
        @JsonProperty("relevance_score")
        Double relevanceScore,
        RerankDocument document
) {
}
