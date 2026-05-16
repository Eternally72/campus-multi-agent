package com.campus.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "campus.vector")
public record VectorProperties(int dimensions, boolean initializeSchema) {
}
