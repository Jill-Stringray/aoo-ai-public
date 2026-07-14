package com.clockshore.aooai.mq;

import com.clockshore.aooai.Utility.ToJsonUtil;
import com.clockshore.aooai.dto.mq.RetrievalSourceDTO;
import com.clockshore.aooai.entity.MessageRetrievalSource;
import com.clockshore.aooai.mapper.MessageRetrievalSourceMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
public class RetrievalSourceMqListener {


    @Resource
    private ToJsonUtil toJsonUtil;


    @Resource
    private MessageRetrievalSourceMapper messageRetrievalSourceMapper;

    @RabbitListener(
            queues = ChatMqConfig.RETRIEVAL_QUEUE,
            containerFactory = ChatMqConfig.RELIABLE_LISTENER_CONTAINER_FACTORY
    )
    @Transactional
    public void retrievalSourceConsume(String payload) {


        RetrievalSourceEvent event =
                toJsonUtil.fromJson(
                        payload,
                        new TypeReference<>() {
                        }
                );

        if (event == null || event.getMessageKey() == null || event.getMessageKey().isBlank()) {
            throw new IllegalArgumentException("retrieval source messageKey is required");
        }
        if (event.getSources() == null) {
            throw new IllegalArgumentException("retrieval source list is required");
        }

        List<MessageRetrievalSource> sources =
                new ArrayList<>();

        int i = 1;
        for (RetrievalSourceDTO source : event.getSources()) {

            MessageRetrievalSource entity =
                    MessageRetrievalSource.builder()
                            .messageKey(event.getMessageKey())
                            .knowledgeBaseId(source.getKnowledgeBaseId())
                            .documentId(source.getDocumentId())
                            .chunkId(source.getChunkId())
                            .rankNo(i)
                            .score(source.getScore())
                            .quoteText(source.getContent())
                            .build();

            sources.add(entity);
            i++;
        }


        if (!sources.isEmpty()) {
            messageRetrievalSourceMapper.batchInsert(sources);
        }
    }

}
