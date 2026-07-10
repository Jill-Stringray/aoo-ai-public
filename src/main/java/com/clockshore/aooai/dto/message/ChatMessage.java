package com.clockshore.aooai.dto.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;




@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * 前端展示dto
 */
public class ChatMessage {

    private Long id;
    private String role;
    private String content;
    private LocalDateTime createTime;
}