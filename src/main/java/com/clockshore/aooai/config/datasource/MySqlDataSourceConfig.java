package com.clockshore.aooai.config.datasource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.jdbc.DataSourceBuilder;

import javax.sql.DataSource;

@Configuration
public class MySqlDataSourceConfig {

    @Primary
    @Bean(name = "datasource")
    public DataSource dataSource() {
        return DataSourceBuilder.create()
                .url("jdbc:mysql://localhost:3306/aoo-ai?useSSL=false&useUnicode=true&characterEncoding=UTF-8")
                .username(System.getenv("MYSQL_USERNAME"))
                .password(System.getenv("MYSQL_PASSWORD"))
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .build();
    }
}