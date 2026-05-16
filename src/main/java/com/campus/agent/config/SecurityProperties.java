package com.campus.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "campus.security")
public record SecurityProperties(String jwtSecret, long tokenTtlMinutes) {
}
