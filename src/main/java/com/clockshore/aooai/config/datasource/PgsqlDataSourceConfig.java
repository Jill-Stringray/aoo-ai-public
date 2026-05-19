package com.clockshore.aooai.config.datasource;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class PgsqlDataSourceConfig {
    @Bean(name = "pgDataSource")
    public DataSource pgDataSource() {
        return DataSourceBuilder.create()
                .url("jdbc:postgresql://localhost:5432/rag")
                .username(System.getenv("POSTGRES_USERNAME"))
                .password(System.getenv("POSTGRES_PASSWORD"))
                .driverClassName("org.postgresql.Driver")
                .build();
    }
}
