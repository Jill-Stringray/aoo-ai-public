package com.clockshore.aooai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AskRequest {
    @NotBlank
    private String question;
    private Long knowledgeBaseId;
    private Long conversationId;
}
