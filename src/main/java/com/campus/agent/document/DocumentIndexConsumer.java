package com.campus.agent.document;

import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
    topic = DocumentEventPublisher.TOPIC,
    consumerGroup = "campus-document-indexer"
)
public class DocumentIndexConsumer implements RocketMQListener<DocumentIndexEvent> {

    private final DocumentIndexingService indexingService;

    @Override
    public void onMessage(DocumentIndexEvent event) {
        indexingService.index(event);
    }
}
