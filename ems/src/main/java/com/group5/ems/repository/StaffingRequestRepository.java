package com.group5.ems.repository;

import com.group5.ems.entity.StaffingRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.persistence.QueryHint;
import java.util.List;

@Repository
public interface StaffingRequestRepository extends JpaRepository<StaffingRequest, Long> {

    List<StaffingRequest> findByStatusOrderByCreatedAtDesc(String status);

    List<StaffingRequest> findByDepartmentIdOrderByCreatedAtDesc(Long departmentId);

    @Query("""
            SELECT sr FROM StaffingRequest sr
            LEFT JOIN FETCH sr.assignedEmployee ae
            LEFT JOIN FETCH ae.user
            LEFT JOIN FETCH sr.processedByUser
            LEFT JOIN FETCH sr.department
            WHERE sr.departmentId = :departmentId
            ORDER BY COALESCE(sr.processedAt, sr.updatedAt, sr.createdAt) DESC
            """)
    List<StaffingRequest> findRecentByDepartmentId(Long departmentId);

    @Query("SELECT sr FROM StaffingRequest sr WHERE sr.status = 'PENDING' ORDER BY sr.createdAt DESC")
    List<StaffingRequest> findAllPendingRequests();
}
