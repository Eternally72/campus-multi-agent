package com.campus.agent.memory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemoryCandidateRepository extends JpaRepository<MemoryCandidate, Long> {

    List<MemoryCandidate> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, MemoryCandidateStatus status);

    Optional<MemoryCandidate> findFirstByUserIdAndMemoryTypeAndMemoryKeyAndMemoryValueAndStatusOrderByCreatedAtDesc(
        Long userId,
        MemoryType memoryType,
        String memoryKey,
        String memoryValue,
        MemoryCandidateStatus status
    );
}
