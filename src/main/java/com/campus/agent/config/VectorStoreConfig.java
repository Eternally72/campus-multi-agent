package com.campus.agent.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
@EnableConfigurationProperties(VectorProperties.class)
public class VectorStoreConfig {

    @Bean
    @ConfigurationProperties("campus.vector.datasource")
    DataSource vectorDataSource() {
        return new HikariDataSource();
    }

    @Bean
    JdbcTemplate vectorJdbcTemplate(@Qualifier("vectorDataSource") DataSource vectorDataSource) {
        return new JdbcTemplate(vectorDataSource);
    }

    @Bean
    @ConditionalOnBean(EmbeddingModel.class)
    VectorStore vectorStore(
        @Qualifier("vectorJdbcTemplate") JdbcTemplate jdbcTemplate,
        EmbeddingModel embeddingModel,
        VectorProperties properties
    ) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
            .dimensions(properties.dimensions())
            .initializeSchema(properties.initializeSchema())
            .build();
    }
}
