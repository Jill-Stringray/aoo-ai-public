package com.clockshore.aooai.dto;


import com.clockshore.aooai.entity.UsageDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponseDto {

    /**
     * 本次返回的文本 delta
     */
    private String content;

    /**
     * 是否结束
     */
    private Boolean finished;

    /**
     * token 使用量
     */
    private UsageDto usage;

    /**
     * finish reason
     */
    private String finishReason;
}