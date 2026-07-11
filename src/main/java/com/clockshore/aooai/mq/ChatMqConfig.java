package com.clockshore.aooai.mq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatMqConfig {

    /**
     * 聊天消息持久化
     */
    public static final String CHAT_EXCHANGE = "chat.message.exchange";
    public static final String CHAT_QUEUE = "chat.message.queue";
    public static final String CHAT_ROUTING_KEY = "chat.message.send";

    @Bean
    public DirectExchange chatExchange() {
        return new DirectExchange(CHAT_EXCHANGE, true, false);
    }

    @Bean
    public Queue chatQueue() {
        return new Queue(CHAT_QUEUE, true);
    }

    @Bean
    public Binding chatBinding() {
        return BindingBuilder.bind(chatQueue()).to(chatExchange()).with(CHAT_ROUTING_KEY);
    }


    /**
     * RAG处理
     */
    public static final String EMBEDDING_EXCHANGE = "ragprocess.message.exchange";
    public static final String EMBEDDING_QUEUE = "ragprocess.message.queue";
    public static final String EMBEDDING_ROUTING_KEY = "ragprocess.message.send";

    @Bean
    public  DirectExchange ragProcessExchange(){
        return new DirectExchange(EMBEDDING_EXCHANGE,true,false);
    }

    @Bean
    public Queue ragProcessQueue(){
        return new Queue(EMBEDDING_QUEUE,true);
    }

    @Bean
    public Binding ragProcessBinding(){
        return BindingBuilder.bind(ragProcessQueue()).to(ragProcessExchange()).with(EMBEDDING_ROUTING_KEY);
    }

    /**
     * RAG召回来源保存
     */
    public static final String RETRIEVAL_EXCHANGE = "retrieval.source.exchange";
    public static final String RETRIEVAL_QUEUE = "retrieval.source.queue";
    public static final String RETRIEVAL_ROUTING_KEY = "retrieval.source.send";


    @Bean
    public DirectExchange retrievalExchange(){
        return new DirectExchange(
                RETRIEVAL_EXCHANGE,
                true,
                false
        );
    }


    @Bean
    public Queue retrievalQueue(){
        return new Queue(
                RETRIEVAL_QUEUE,
                true
        );
    }


    @Bean
    public Binding retrievalBinding(){
        return BindingBuilder
                .bind(retrievalQueue())
                .to(retrievalExchange())
                .with(RETRIEVAL_ROUTING_KEY);
    }
}
