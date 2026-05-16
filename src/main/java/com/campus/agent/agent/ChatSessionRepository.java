package com.campus.agent.agent;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    List<ChatSession> findByOwnerIdOrderByUpdatedAtDesc(Long ownerId);
}
