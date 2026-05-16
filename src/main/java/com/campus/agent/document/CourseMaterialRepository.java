package com.campus.agent.document;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseMaterialRepository extends JpaRepository<CourseMaterial, Long> {

    List<CourseMaterial> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
}
