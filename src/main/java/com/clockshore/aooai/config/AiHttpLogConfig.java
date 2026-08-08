package com.clockshore.aooai.config;

import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiHttpLogConfig {

    @Bean
    public RestClientCustomizer restClientCustomizer() {
        return builder -> builder
                .requestInterceptor((request, body, execution) -> {

                    System.out.println("====== AI REQUEST ======");
                    System.out.println("URL: " + request.getURI());
                    System.out.println("Headers: " + request.getHeaders());
                    System.out.println("Body:");
                    System.out.println(new String(body));

                    return execution.execute(request, body);
                });
    }
}