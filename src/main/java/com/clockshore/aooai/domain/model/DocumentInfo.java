package com.clockshore.aooai.domain.model;

import com.clockshore.aooai.domain.enums.DocumentStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DocumentInfo {
    private Long id;
    private Long knowledgeBaseId;
    private String fileName;
    private DocumentStatus status;
}
