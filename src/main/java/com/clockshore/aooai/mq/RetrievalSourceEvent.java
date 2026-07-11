package com.clockshore.aooai.mq;

import com.clockshore.aooai.entity.MessageRetrievalSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievalSourceEvent {


    /**
     * AI回答消息ID
     */
    private Long messageId;


    /**
     * 召回来源
     */
    private List<MessageRetrievalSource> sources;

}
