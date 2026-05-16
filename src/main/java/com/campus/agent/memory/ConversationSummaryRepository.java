package com.campus.agent.memory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConversationSummaryRepository extends JpaRepository<ConversationSummary, Long> {

    Optional<ConversationSummary> findBySessionId(Long sessionId);
}
