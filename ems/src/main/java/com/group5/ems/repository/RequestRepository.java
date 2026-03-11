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

    // Thêm vào RequestRepository.java
    @Query("SELECT r FROM Request r " +
            "JOIN FETCH r.employee e " +
            "JOIN FETCH e.user u " +
            "LEFT JOIN FETCH e.position p " +
            "WHERE r.leaveType IS NOT NULL AND r.status = :status " +
            "ORDER BY r.createdAt DESC")
    Page<Request> findLeaveRequestsByStatus(@Param("status") String status, Pageable pageable);

    @Query("SELECT r FROM Request r " +
            "JOIN FETCH r.employee e " +
            "JOIN FETCH e.user u " +
            "LEFT JOIN FETCH e.position p " +
            "WHERE r.leaveType IS NOT NULL " +
            "ORDER BY r.createdAt DESC")
    Page<Request> findAllLeaveRequests(Pageable pageable);

    // Đếm số NV đang nghỉ hôm nay
    @Query("SELECT COUNT(r) FROM Request r WHERE r.status = 'APPROVED' " +
            "AND r.leaveFrom <= CURRENT_DATE AND r.leaveTo >= CURRENT_DATE")
    int countOnLeaveToday();

    // Đếm số request được approve hôm nay
    @Query("SELECT COUNT(r) FROM Request r WHERE r.status = 'APPROVED' " +
            "AND DATE(r.approvedAt) = CURRENT_DATE")
    int countApprovedToday();

    // Methods for LeaveApprovalService
    List<Request> findByStatusOrderByCreatedAtDesc(String status);
    List<Request> findByStatusOrderByApprovedAtDesc(String status);
    List<Request> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);
    
    // Alternative queries without leaveType restriction
    @Query("SELECT r FROM Request r " +
            "JOIN FETCH r.employee e " +
            "JOIN FETCH e.user u " +
            "LEFT JOIN FETCH e.position p " +
            "WHERE r.status = :status " +
            "ORDER BY r.createdAt DESC")
    Page<Request> findRequestsByStatusWithoutLeaveTypeFilter(@Param("status") String status, Pageable pageable);

    @Query("SELECT r FROM Request r " +
            "JOIN FETCH r.employee e " +
            "JOIN FETCH e.user u " +
            "LEFT JOIN FETCH e.position p " +
            "ORDER BY r.createdAt DESC")
    Page<Request> findAllRequestsWithoutLeaveTypeFilter(Pageable pageable);
    @Query("SELECT COUNT(r) FROM Request r WHERE r.status = :status " +
           "AND r.approvedAt BETWEEN :startDate AND :endDate")
    long countByStatusAndApprovedAtBetween(@Param("status") String status, 
                                          @Param("startDate") LocalDateTime startDate, 
                                          @Param("endDate") LocalDateTime endDate);

}

