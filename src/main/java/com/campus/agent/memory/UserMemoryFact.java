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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "user_memory_facts")
public class UserMemoryFact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(nullable = false, length = 120)
    private String factKey;

    @Column(nullable = false, length = 1000)
    private String factValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MemoryStatus status = MemoryStatus.ACTIVE;

    @Column(nullable = false)
    private double confidence;

    @Column(length = 80)
    private String source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_session_id")
    private ChatSession sourceSession;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replaced_by_id")
    private UserMemoryFact replacedBy;

    private Instant expiresAt;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    private Instant forgottenAt;
}
