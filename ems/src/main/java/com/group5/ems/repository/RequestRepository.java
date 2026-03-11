package com.group5.ems.repository;

import com.group5.ems.entity.Request;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RequestRepository extends JpaRepository<Request, Long> {

    List<Request> findByEmployeeId(Long employeeId);

    List<Request> findByEmployeeIdAndStatus(Long employeeId, String status);

    List<Request> findByStatus(String status);

    List<Request> findByEmployeeIdAndLeaveTypeIsNotNull(Long employeeId);
    List<Request> findByEmployeeIdAndLeaveTypeIsNotNullOrderByCreatedAtDesc(Long employeeId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(r) FROM Request r WHERE r.status = :status AND r.requestType.category = :category")
    int countByStatusAndRequestTypeCategory(@org.springframework.data.repository.query.Param("status") String status, @org.springframework.data.repository.query.Param("category") String category);
    
    List<Request> findByEmployeeDepartmentIdAndLeaveTypeIsNotNullOrderByCreatedAtDesc(Long departmentId);
  
    @Query("SELECT r FROM Request r " +
           "JOIN FETCH r.employee e " +
           "JOIN FETCH e.user u " +
           "LEFT JOIN FETCH e.position p " +
           "WHERE r.status = 'PENDING' OR " +
           "(r.approvedAt IS NOT NULL AND r.approvedAt >= :since) " +
           "ORDER BY r.createdAt DESC")
    List<Request> findRecentActivities(@Param("since") LocalDateTime since);
    
    @Query("SELECT COUNT(r) FROM Request r WHERE r.status = :status")
    Long countByStatus(@Param("status") String status);
}

