package com.clockshore.aooai.dto.rerank;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record RerankRequest(
        String model,
        String query,
        List<String> documents,
        @JsonProperty("top_n")
        Integer topN,
        @JsonProperty("return_documents")
        Boolean returnDocuments
) {
}
