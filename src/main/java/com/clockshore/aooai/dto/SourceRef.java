package com.clockshore.aooai.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SourceRef {
    private String documentName;
    private Integer pageNumber;
    private String chunkId;
}
