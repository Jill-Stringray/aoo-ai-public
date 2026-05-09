package com.clockshore.aooai.dto;

import lombok.Data;

@Data
public class ConversationCreateDto {

    /**
     * 可选；为空则创建无标题会话
     */
    private String title;
}
