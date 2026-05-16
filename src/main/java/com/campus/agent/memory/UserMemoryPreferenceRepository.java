package com.campus.agent.memory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserMemoryPreferenceRepository extends JpaRepository<UserMemoryPreference, Long> {

    List<UserMemoryPreference> findByUserIdAndStatusOrderByUpdatedAtDesc(Long userId, MemoryStatus status);

    Optional<UserMemoryPreference> findFirstByUserIdAndMemoryKeyAndStatusOrderByUpdatedAtDesc(Long userId, String memoryKey, MemoryStatus status);
}
