package com.clockshore.aooai.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {


    @Bean
    @Qualifier("siliconFlow")
    public WebClient siliconFlowWebClient(
            WebClient.Builder builder
    ){

        return builder
                .baseUrl("https://api.siliconflow.cn")
                .build();
    }

    @Bean
    @Qualifier("doclingService")
    public WebClient doclingServiceWebClient(
            WebClient.Builder builder,
            @Value("${docling.service.base-url:http://localhost:8000}") String baseUrl
    ) {
        return builder
                .baseUrl(baseUrl)
                .build();
    }
}
