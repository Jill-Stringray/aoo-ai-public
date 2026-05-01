package com.clockshore.aooai.Entity;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocalMqMessage {

    private Long id;
    private String eventKey;
    private String topic;
    private String routingKey;
    private String payload;
    private String status;
    private Integer retryCount;
    private LocalDateTime nextRetryAt;
    private String failReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
