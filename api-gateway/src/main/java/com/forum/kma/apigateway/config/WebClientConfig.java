package com.forum.kma.apigateway.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    @Bean
    @LoadBalanced // RẤT QUAN TRỌNG: Sử dụng Load Balancer để gọi qua Eureka
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public WebClient webClient(WebClient.Builder loadBalancedWebClientBuilder) {
        // Sử dụng builder để có tính năng Load Balancing
        return loadBalancedWebClientBuilder.build();
    }
}