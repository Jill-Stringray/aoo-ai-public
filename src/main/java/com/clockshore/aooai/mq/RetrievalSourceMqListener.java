package com.clockshore.aooai.mq;

import com.clockshore.aooai.entity.MessageRetrievalSource;
import com.clockshore.aooai.mapper.MessageRetrievalSourceMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class RetrievalSourceMqListener {


    @Resource
    private ObjectMapper objectMapper;


    @Resource
    private MessageRetrievalSourceMapper messageRetrievalSourceMapper;



    @RabbitListener(
            queues = ChatMqConfig.RETRIEVAL_QUEUE
    )
    @Transactional
    public void retrievalSourceConsume(String payload) {

        RetrievalSourceEvent event = parse(payload);


        List<MessageRetrievalSource> sources =
                event.getSources()
                        .stream()
                        .map(source ->
                                MessageRetrievalSource.builder()
                                        .messageId(event.getMessageId())
                                        .knowledgeBaseId(source.getKnowledgeBaseId())
                                        .documentId(source.getDocumentId())
                                        .chunkId(source.getChunkId())
                                        .rankNo(source.getRankNo())
                                        .score(source.getScore())
                                        .rerankScore(source.getRerankScore())
                                        .quoteText(source.getQuoteText())
                                        .build()
                        )
                        .toList();


        if (!sources.isEmpty()) {
            messageRetrievalSourceMapper.batchInsert(sources);
        }
    }



    private RetrievalSourceEvent parse(String payload) {

        try {

            return objectMapper.readValue(
                    payload,
                    RetrievalSourceEvent.class
            );

        } catch (JsonProcessingException e) {

            throw new IllegalStateException(
                    "parse retrieval source event failed",
                    e
            );
        }
    }

}