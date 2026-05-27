package com.clockshore.aooai.domain.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KnowledgeBase {
    private Long id;
    private String name;
    private String description;
    private String owner;
}
