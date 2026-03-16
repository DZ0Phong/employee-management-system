package com.group5.ems.repository;

import com.group5.ems.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByPerformedBy(Long performedBy, Pageable pageable);

    Page<AuditLog> findByEntityNameAndEntityId(String entityName, Long entityId, Pageable pageable);

    Page<AuditLog> findByAction(String action, Pageable pageable);

    long countByAction(String action);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    void deleteByCreatedAtBefore(LocalDateTime cutoff);
}
