package com.campus.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "campus.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
