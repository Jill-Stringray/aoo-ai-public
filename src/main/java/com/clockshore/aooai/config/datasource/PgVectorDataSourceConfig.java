package com.clockshore.aooai.config.datasource;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class PgVectorDataSourceConfig {

    @Bean
    @ConfigurationProperties("app.datasource.pgvector")
    public DataSourceProperties pgVectorDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "pgVectorDataSource")
    @ConfigurationProperties("app.datasource.pgvector.hikari")
    public DataSource pgVectorDataSource(@Qualifier("pgVectorDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }
}
