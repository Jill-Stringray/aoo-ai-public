package com.clockshore.aooai.mq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class ChatMqConfig {

    public static final String RELIABLE_LISTENER_CONTAINER_FACTORY = "reliableRabbitListenerContainerFactory";
    public static final String DLX_EXCHANGE = "aoo.ai.dlx.exchange";

    private static final String DEAD_LETTER_EXCHANGE_ARGUMENT = "x-dead-letter-exchange";
    private static final String DEAD_LETTER_ROUTING_KEY_ARGUMENT = "x-dead-letter-routing-key";

    /**
     * 聊天消息持久化
     */
    public static final String CHAT_EXCHANGE = "chat.message.exchange";
    public static final String CHAT_QUEUE = "chat.message.queue";
    public static final String CHAT_DLQ = "chat.message.queue.dlq";
    public static final String CHAT_ROUTING_KEY = "chat.message.send";
    public static final String CHAT_DLQ_ROUTING_KEY = "chat.message.dlq";

    @Bean
    public DirectExchange chatExchange() {
        return new DirectExchange(CHAT_EXCHANGE, true, false);
    }

    @Bean
    public Queue chatQueue() {
        return QueueBuilder.durable(CHAT_QUEUE)
                .withArgument(DEAD_LETTER_EXCHANGE_ARGUMENT, DLX_EXCHANGE)
                .withArgument(DEAD_LETTER_ROUTING_KEY_ARGUMENT, CHAT_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding chatBinding() {
        return BindingBuilder.bind(chatQueue()).to(chatExchange()).with(CHAT_ROUTING_KEY);
    }

    @Bean
    public Queue chatDeadLetterQueue() {
        return QueueBuilder.durable(CHAT_DLQ).build();
    }

    @Bean
    public Binding chatDeadLetterBinding() {
        return BindingBuilder.bind(chatDeadLetterQueue()).to(deadLetterExchange()).with(CHAT_DLQ_ROUTING_KEY);
    }


    /**
     * RAG处理
     */
    public static final String EMBEDDING_EXCHANGE = "ragprocess.message.exchange";
    public static final String EMBEDDING_QUEUE = "ragprocess.message.queue";
    public static final String EMBEDDING_DLQ = "ragprocess.message.queue.dlq";
    public static final String EMBEDDING_ROUTING_KEY = "ragprocess.message.send";
    public static final String EMBEDDING_DLQ_ROUTING_KEY = "ragprocess.message.dlq";

    @Bean
    public  DirectExchange ragProcessExchange(){
        return new DirectExchange(EMBEDDING_EXCHANGE,true,false);
    }

    @Bean
    public Queue ragProcessQueue(){
        return QueueBuilder.durable(EMBEDDING_QUEUE)
                .withArgument(DEAD_LETTER_EXCHANGE_ARGUMENT, DLX_EXCHANGE)
                .withArgument(DEAD_LETTER_ROUTING_KEY_ARGUMENT, EMBEDDING_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding ragProcessBinding(){
        return BindingBuilder.bind(ragProcessQueue()).to(ragProcessExchange()).with(EMBEDDING_ROUTING_KEY);
    }

    @Bean
    public Queue ragProcessDeadLetterQueue(){
        return QueueBuilder.durable(EMBEDDING_DLQ).build();
    }

    @Bean
    public Binding ragProcessDeadLetterBinding(){
        return BindingBuilder.bind(ragProcessDeadLetterQueue()).to(deadLetterExchange()).with(EMBEDDING_DLQ_ROUTING_KEY);
    }

    /**
     * RAG召回来源保存
     */
    public static final String RETRIEVAL_EXCHANGE = "retrieval.source.exchange";
    public static final String RETRIEVAL_QUEUE = "retrieval.source.queue";
    public static final String RETRIEVAL_DLQ = "retrieval.source.queue.dlq";
    public static final String RETRIEVAL_ROUTING_KEY = "retrieval.source.send";
    public static final String RETRIEVAL_DLQ_ROUTING_KEY = "retrieval.source.dlq";


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
        return QueueBuilder.durable(RETRIEVAL_QUEUE)
                .withArgument(DEAD_LETTER_EXCHANGE_ARGUMENT, DLX_EXCHANGE)
                .withArgument(DEAD_LETTER_ROUTING_KEY_ARGUMENT, RETRIEVAL_DLQ_ROUTING_KEY)
                .build();
    }


    @Bean
    public Binding retrievalBinding(){
        return BindingBuilder
                .bind(retrievalQueue())
                .to(retrievalExchange())
                .with(RETRIEVAL_ROUTING_KEY);
    }

    @Bean
    public Queue retrievalDeadLetterQueue(){
        return QueueBuilder.durable(RETRIEVAL_DLQ).build();
    }

    @Bean
    public Binding retrievalDeadLetterBinding(){
        return BindingBuilder
                .bind(retrievalDeadLetterQueue())
                .to(deadLetterExchange())
                .with(RETRIEVAL_DLQ_ROUTING_KEY);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    @Bean(RELIABLE_LISTENER_CONTAINER_FACTORY)
    public SimpleRabbitListenerContainerFactory reliableRabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            RetryOperationsInterceptor rabbitRetryInterceptor
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(rabbitRetryInterceptor);
        return factory;
    }

    @Bean
    public RetryOperationsInterceptor rabbitRetryInterceptor() {
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        //以下是四种不可重试的错误
        //1.参数非法
        //2.状态非法
        //3.消息转化失败
        //4.数据完整性异常不重试，比如唯一索引冲突、字段约束失败
        retryableExceptions.put(IllegalArgumentException.class, false);
        retryableExceptions.put(IllegalStateException.class, false);
        retryableExceptions.put(MessageConversionException.class, false);
        retryableExceptions.put(DataIntegrityViolationException.class, false);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(3, retryableExceptions, true, true);

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000);
        backOffPolicy.setMultiplier(2.0);
        backOffPolicy.setMaxInterval(10000);

        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        return RetryInterceptorBuilder.stateless()
                .retryOperations(retryTemplate)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build();
    }
}
