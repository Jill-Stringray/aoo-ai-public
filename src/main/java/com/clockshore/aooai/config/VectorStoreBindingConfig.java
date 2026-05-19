package com.clockshore.aooai.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class VectorStoreBindingConfig {

    @Bean
    @ConditionalOnMissingBean(VectorStore.class)
    public VectorStore vectorStore(@Qualifier("pgDataSource") DataSource pgDataSource,
                                   EmbeddingModel embeddingModel) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(pgDataSource);
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .initializeSchema(true)
                .dimensions(1536)
                .build();
    }
}
