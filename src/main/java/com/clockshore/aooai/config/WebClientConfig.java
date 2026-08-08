package com.clockshore.aooai.config;

import org.springframework.beans.factory.annotation.Qualifier;
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
}