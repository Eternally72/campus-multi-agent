package com.campus.agent.document;

import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DocumentEventPublisher {

    private static final String TOPIC = "campus-document-index";

    private final ObjectProvider<RocketMQTemplate> rocketMQTemplateProvider;

    public void publishIndexRequested(DocumentIndexEvent event) {
        RocketMQTemplate template = rocketMQTemplateProvider.getIfAvailable();
        if (template != null) {
            try {
                template.convertAndSend(TOPIC, event);
            } catch (RuntimeException ignored) {
                // The first version indexes synchronously; MQ is an extension point when RocketMQ is online.
            }
        }
    }
}
