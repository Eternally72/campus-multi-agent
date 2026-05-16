package com.campus.agent.memory;

import com.campus.agent.agent.ChatSession;
import com.campus.agent.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "memory_candidates")
public class MemoryCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private ChatSession session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MemoryType memoryType;

    @Column(nullable = false, length = 120)
    private String memoryKey;

    @Column(nullable = false, length = 1000)
    private String memoryValue;

    @Column(length = 80)
    private String category;

    @Column(nullable = false)
    private double confidence;

    @Column(length = 500)
    private String reason;

    @Column(length = 80)
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MemoryCandidateStatus status = MemoryCandidateStatus.PENDING;

    private Instant expiresAt;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant decidedAt;
}
