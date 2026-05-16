package com.campus.agent.document;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "campus.upload")
public record DocumentProperties(int maxCharacters) {
}
