package com.clockshore.aooai.config;

import javax.sql.DataSource;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@RequiredArgsConstructor
public class VectorStoreConfig {

    private final EmbeddingModel embeddingModel;

    @Bean
    public VectorStore vectorStore(
            //创建 VectorStore 时，不要用默认的 DataSource，而是用名字为 pgDataSource 的那个 Bean
            @Qualifier("pgDataSource")
            DataSource pgDataSource) {

        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(pgDataSource);

        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(1024)
                .initializeSchema(true)
                .build();
    }
}