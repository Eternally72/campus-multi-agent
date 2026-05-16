package com.campus.agent.memory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserMemoryFactRepository extends JpaRepository<UserMemoryFact, Long> {

    List<UserMemoryFact> findByUserIdAndStatusOrderByUpdatedAtDesc(Long userId, MemoryStatus status);

    Optional<UserMemoryFact> findFirstByUserIdAndFactKeyAndStatusOrderByUpdatedAtDesc(Long userId, String factKey, MemoryStatus status);

    List<UserMemoryFact> findByStatusAndExpiresAtBefore(MemoryStatus status, Instant now);
}
