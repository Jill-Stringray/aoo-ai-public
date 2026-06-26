package com.clockshore.aooai.mq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatMqConfig {

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

}
