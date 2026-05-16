package com.campus.agent.document;

import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DocumentEventPublisher {

    public static final String TOPIC = "campus-document-index";

    private final ObjectProvider<RocketMQTemplate> rocketMQTemplateProvider;

    public void publishIndexRequested(DocumentIndexEvent event) {
        RocketMQTemplate template = rocketMQTemplateProvider.getIfAvailable();
        if (template == null) {
            throw new IllegalStateException("RocketMQTemplate is not available");
        }
        template.convertAndSend(TOPIC, event);
    }
}
