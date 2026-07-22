package com.clockshore.aoogateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtGlobalFilter implements GlobalFilter {


    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain) {


        String token =
                exchange.getRequest()
                        .getHeaders()
                        .getFirst("Authorization");


        if(token == null){
            return chain.filter(exchange);
        }


        // TODO JWT验证


        return chain.filter(exchange);
    }
}