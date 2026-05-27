package com.clockshore.aooai.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AskResponse {
    private String answer;
    private List<SourceRef> sources;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
}
