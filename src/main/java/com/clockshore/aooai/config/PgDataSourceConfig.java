package com.clockshore.aooai.config;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//给spring ai的vectorStore使用
@Configuration
public class PgDataSourceConfig {

    @Bean
    @ConfigurationProperties("postgres.datasource")
    public DataSourceProperties pgDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "pgDataSource")
    public DataSource pgDataSource() {
        return pgDataSourceProperties()
                .initializeDataSourceBuilder()
                .build();
    }
}