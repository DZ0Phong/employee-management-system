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

    @Query("SELECT r FROM Request r " +
            "JOIN FETCH r.employee e " +
            "JOIN FETCH e.user u " +
            "LEFT JOIN FETCH e.position p " +
            "WHERE r.status = :status " +
            "ORDER BY r.createdAt DESC")
    List<Request> findByStatusWithDetails(@Param("status") String status);

    List<Request> findByEmployeeIdAndLeaveTypeIsNotNull(Long employeeId);
    List<Request> findByEmployeeIdAndLeaveTypeIsNotNullOrderByCreatedAtDesc(Long employeeId);

    @Query("SELECT COUNT(r) FROM Request r WHERE r.status = :status AND r.requestType.category = :category")
    int countByStatusAndRequestTypeCategory(@Param("status") String status, @Param("category") String category);

    @Query("SELECT COUNT(r) FROM Request r JOIN r.requestType rt " +
            "WHERE r.status = :status AND rt.code IN :codes")
    long countByStatusAndRequestTypeCodeIn(@Param("status") String status, @Param("codes") List<String> codes);


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


    // ── Pageable queries for HR leave page (DB-level filtering) ──

    // ── Payroll Aggregation queries ──

    /**
     * Finds approved unpaid leave requests overlapping a given date range for a specific employee.
     * Uses Request.startDate / Request.endDate (Instant fields).
     */
    @Query("SELECT r FROM Request r JOIN r.requestType rt " +
           "WHERE r.employeeId = :empId AND r.status = 'APPROVED' " +
           "AND rt.code = 'LEAVE_UNPAID' " +
           "AND r.startDate <= :endInstant AND r.endDate >= :startInstant")
    List<Request> findApprovedUnpaidLeave(@Param("empId") Long empId,
                                          @Param("startInstant") java.time.Instant startInstant,
                                          @Param("endInstant") java.time.Instant endInstant);

    /**
     * Finds approved overtime requests overlapping a given date range for a specific employee.
     * Uses Request.startDate / Request.endDate (Instant fields).
     */
    @Query("SELECT r FROM Request r JOIN r.requestType rt " +
           "WHERE r.employeeId = :empId AND r.status = 'APPROVED' " +
           "AND rt.code = 'ATT_OVERTIME' " +
           "AND r.startDate <= :endInstant AND r.endDate >= :startInstant")
    List<Request> findApprovedOvertime(@Param("empId") Long empId,
                                       @Param("startInstant") java.time.Instant startInstant,
                                       @Param("endInstant") java.time.Instant endInstant);
}