package com.group5.ems.repository;

import com.group5.ems.entity.StaffingRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StaffingRequestRepository extends JpaRepository<StaffingRequest, Long> {

    List<StaffingRequest> findByStatusOrderByCreatedAtDesc(String status);

    List<StaffingRequest> findByDepartmentIdOrderByCreatedAtDesc(Long departmentId);

    @Query("SELECT sr FROM StaffingRequest sr WHERE sr.status = 'PENDING' ORDER BY sr.createdAt DESC")
    List<StaffingRequest> findAllPendingRequests();
}
