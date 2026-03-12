package com.group5.ems.repository;

import com.group5.ems.entity.Request;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query("SELECT COUNT(r) FROM Request r WHERE r.status = :status AND r.requestType.category = :category")
    int countByStatusAndRequestTypeCategory(@Param("status") String status, @Param("category") String category);
    
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

    // ── Pageable queries for HR leave page (DB-level filtering) ──

    @Query("SELECT r FROM Request r JOIN FETCH r.employee e JOIN FETCH e.user u " +
           "LEFT JOIN FETCH e.department d JOIN FETCH r.requestType rt " +
           "WHERE r.status = 'PENDING' AND rt.category = 'ATTENDANCE' " +
           "ORDER BY r.createdAt DESC")
    List<Request> findPendingLeaveRequests();

    @Query(value = "SELECT r FROM Request r JOIN r.employee e JOIN e.user u " +
           "LEFT JOIN e.department d JOIN r.requestType rt " +
           "WHERE r.status <> 'PENDING' AND rt.category = 'ATTENDANCE' " +
           "ORDER BY r.createdAt DESC",
           countQuery = "SELECT COUNT(r) FROM Request r JOIN r.requestType rt " +
           "WHERE r.status <> 'PENDING' AND rt.category = 'ATTENDANCE'")
    Page<Request> findLeaveHistory(Pageable pageable);

    // ── Pageable queries for HR workflow requests ──

    @Query(value = "SELECT r FROM Request r JOIN r.employee e JOIN e.user u " +
           "LEFT JOIN e.department d JOIN r.requestType rt " +
           "WHERE rt.category = 'HR_STATUS' ORDER BY r.createdAt DESC",
           countQuery = "SELECT COUNT(r) FROM Request r JOIN r.requestType rt WHERE rt.category = 'HR_STATUS'")
    Page<Request> findWorkflowRequests(Pageable pageable);
}
