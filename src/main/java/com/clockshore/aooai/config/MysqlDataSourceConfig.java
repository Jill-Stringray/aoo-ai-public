package com.clockshore.aooai.config;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class MysqlDataSourceConfig {
    //这里显式表达了MysqlDataSource，用来配置双数据源
    //添加了@Primary，成为主数据源
    /*
        如果只手动注册 pgDataSource
        ↓
        Spring Boot 发现容器里已经有 DataSource
        ↓
        不再自动创建 MySQL DataSource
        ↓
        MyBatis 自动绑定到唯一的 DataSource（PostgreSQL）
        ↓
        MySQL SQL 被 PostgreSQL 执行
        ↓
        org.postgresql.util.PSQLException
     */

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties mysqlDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "mysqlDataSource")
    @Primary
    public DataSource mysqlDataSource() {
        return mysqlDataSourceProperties()
                .initializeDataSourceBuilder()
                .build();
    }
}