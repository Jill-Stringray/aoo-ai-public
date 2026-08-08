package com.clockshore.aooai.dto.rerank;

import java.util.List;

public record RerankResponse(
        List<RerankResult> results
) {
}
