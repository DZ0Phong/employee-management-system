package com.group5.ems.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.group5.ems.entity.Request;

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

    @Query("SELECT r FROM Request r JOIN FETCH r.employee e JOIN FETCH e.user u " +
            "LEFT JOIN FETCH e.department d JOIN FETCH r.requestType rt " +
            "WHERE r.status = 'PENDING' AND r.step = 'WAITING_HR' AND rt.category = 'ATTENDANCE' " +
            "AND (:departmentId IS NULL OR d.id = :departmentId) " +
            "AND (:leaveType IS NULL OR rt.code = :leaveType) " +
            "AND (:search IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "     OR LOWER(e.employeeCode) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY r.createdAt DESC")
    List<Request> findPendingLeaveRequests(
            @Param("departmentId") Long departmentId,
            @Param("leaveType") String leaveType,
            @Param("search") String search);

    @Query("SELECT r FROM Request r JOIN FETCH r.employee e JOIN FETCH e.user u " +
            "LEFT JOIN FETCH e.department d JOIN FETCH r.requestType rt " +
            "WHERE r.status = 'PENDING' AND r.step = 'WAITING_HRM' AND rt.category = 'ATTENDANCE' " +
            "AND (:departmentId IS NULL OR d.id = :departmentId) " +
            "AND (:leaveType IS NULL OR rt.code = :leaveType) " +
            "AND (:search IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "     OR LOWER(e.employeeCode) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY r.createdAt DESC")
    List<Request> findHrmPendingLeaveRequests(
            @Param("departmentId") Long departmentId,
            @Param("leaveType") String leaveType,
            @Param("search") String search);

    @Query(value = "SELECT r FROM Request r JOIN r.employee e JOIN e.user u " +
            "LEFT JOIN e.department d JOIN r.requestType rt " +
            "WHERE r.status <> 'PENDING' AND rt.category = 'ATTENDANCE' " +
            "ORDER BY r.updatedAt DESC, r.createdAt DESC",
            countQuery = "SELECT COUNT(r) FROM Request r JOIN r.requestType rt " +
                    "WHERE r.status <> 'PENDING' AND rt.category = 'ATTENDANCE'")
    Page<Request> findLeaveHistory(Pageable pageable);

        // ── Pageable queries for HR workflow requests ──

        @Query(value = "SELECT r FROM Request r JOIN r.employee e JOIN e.user u " +
                        "LEFT JOIN e.department d JOIN r.requestType rt " +
                        "WHERE rt.category = 'HR_STATUS' ORDER BY r.createdAt DESC", countQuery = "SELECT COUNT(r) FROM Request r JOIN r.requestType rt WHERE rt.category = 'HR_STATUS'")
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
            "LEFT JOIN FETCH e.department d " +
            "WHERE r.status = :status " +
            "ORDER BY r.createdAt DESC")
    Page<Request> findRequestsByStatusWithoutLeaveTypeFilter(@Param("status") String status, Pageable pageable);

    @Query("SELECT r FROM Request r " +
            "JOIN FETCH r.employee e " +
            "JOIN FETCH e.user u " +
            "LEFT JOIN FETCH e.position p " +
            "LEFT JOIN FETCH e.department d " +
            "WHERE r.status = 'APPROVED' " +
            "ORDER BY r.approvedAt DESC")
    Page<Request> findApprovedRequestsOrderByApprovedAt(Pageable pageable);

    @Query("SELECT r FROM Request r " +
            "JOIN FETCH r.employee e " +
            "JOIN FETCH e.user u " +
            "LEFT JOIN FETCH e.position p " +
            "LEFT JOIN FETCH e.department d " +
            "WHERE r.status = 'REJECTED' " +
            "ORDER BY r.approvedAt DESC")
    Page<Request> findRejectedRequestsOrderByApprovedAt(Pageable pageable);

    @Query("SELECT r FROM Request r " +
            "JOIN FETCH r.employee e " +
            "JOIN FETCH e.user u " +
            "LEFT JOIN FETCH e.position p " +
            "LEFT JOIN FETCH e.department d " +
            "WHERE r.status IN ('APPROVED', 'REJECTED') " +
            "ORDER BY r.approvedAt DESC")
    Page<Request> findHistoryRequestsOrderByApprovedAt(Pageable pageable);

    @Query("SELECT r FROM Request r " +
            "JOIN FETCH r.employee e " +
            "JOIN FETCH e.user u " +
            "LEFT JOIN FETCH e.position p " +
            "LEFT JOIN FETCH e.department d " +
            "ORDER BY r.createdAt DESC")
    Page<Request> findAllRequestsWithoutLeaveTypeFilter(Pageable pageable);

        @Query("SELECT COUNT(r) FROM Request r WHERE r.status = :status " +
                        "AND r.approvedAt BETWEEN :startDate AND :endDate")
        long countByStatusAndApprovedAtBetween(@Param("status") String status,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        @Query("SELECT COUNT(r) FROM Request r WHERE r.createdAt BETWEEN :startDate AND :endDate")
        long countByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        @Query("SELECT COUNT(r) FROM Request r WHERE r.status = :status " +
                        "AND r.leaveFrom <= :date AND r.leaveTo >= :date")
        long countByStatusAndLeaveFromLessThanEqualAndLeaveToGreaterThanEqual(
                        @Param("status") String status,
                        @Param("date") java.time.LocalDate date1,
                        @Param("date") java.time.LocalDate date2);

        @Query(value = "SELECT r FROM Request r WHERE r.status = :status " +
                        "AND r.leaveTo > :date ORDER BY r.leaveTo ASC", nativeQuery = false)
        List<Request> findByStatusAndLeaveToGreaterThanOrderByLeaveToAsc(
                        @Param("status") String status,
                        @Param("date") java.time.LocalDate date);

        // ── Pageable queries for HR leave page (DB-level filtering) ──

        List<Request> findByRequestType_CodeInOrderByCreatedAtDesc(List<String> codes);
    // Find overlapping leave requests
    @Query("SELECT r FROM Request r " +
           "JOIN FETCH r.employee e " +
           "JOIN FETCH e.user u " +
           "WHERE r.status = :status " +
           "AND r.leaveFrom <= :endDate " +
           "AND r.leaveTo >= :startDate " +
           "ORDER BY r.leaveFrom ASC")
    List<Request> findOverlappingLeaveRequests(
            @Param("status") String status,
            @Param("startDate") java.time.LocalDate startDate,
            @Param("endDate") java.time.LocalDate endDate);

    // Method for Recent Activities - filter by category
    @Query("SELECT r FROM Request r " +
           "JOIN FETCH r.employee e " +
           "JOIN FETCH e.user u " +
           "LEFT JOIN FETCH e.department d " +
           "LEFT JOIN FETCH e.position p " +
           "JOIN FETCH r.requestType rt " +
           "WHERE rt.category = :category " +
           "AND (r.status = 'PENDING' OR r.updatedAt >= :since) " +
           "ORDER BY CASE WHEN r.status = 'PENDING' THEN 0 ELSE 1 END, r.createdAt DESC")
    List<Request> findRecentRequestsByCategory(
            @Param("category") String category,
            @Param("since") LocalDateTime since);


        // ── Payroll Aggregation queries ──
        long countByRequestType_CodeInAndStatus(List<String> codes, String status);

        // ── Payroll Aggregation queries ──

        /**
         * Finds approved unpaid leave requests overlapping a given date range for a
         * specific employee.
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
         * Finds approved overtime requests overlapping a given date range for a
         * specific employee.
         * Uses Request.startDate / Request.endDate (Instant fields).
         */
        @Query("SELECT r FROM Request r JOIN r.requestType rt " +
                        "WHERE r.employeeId = :empId AND r.status = 'APPROVED' " +
                        "AND rt.code = 'ATT_OVERTIME' " +
                        "AND r.startDate <= :endInstant AND r.endDate >= :startInstant")
        List<Request> findApprovedOvertime(@Param("empId") Long empId,
                        @Param("startInstant") java.time.Instant startInstant,
                        @Param("endInstant") java.time.Instant endInstant);

        // Missing methods that are used by other services


        /**
         * Finds approved overtime requests overlapping a given date range for a
         * specific employee.
         * Uses Request.startDate / Request.endDate (Instant fields).
         */
    // Missing methods that are used by other services
    List<Request> findByRequestType_CodeOrderByCreatedAtDesc(String code);

    long countByRequestType_CodeAndStatus(String code, String status);

    long countByStatusAndRequestTypeCodeIn(String status, List<String> codes);

    @Query("SELECT COUNT(r) FROM Request r WHERE r.status = :status AND r.step = 'WAITING_HR' AND r.requestType.code IN :codes")
    long countByStatusAndStepWaitingHRAndRequestTypeCodeIn(@Param("status") String status, @Param("codes") List<String> codes);

    @Query("SELECT r FROM Request r " +
           "JOIN FETCH r.employee e " +
           "WHERE e.id IN :employeeIds " +
           "AND r.status = 'APPROVED' " +
           "AND r.leaveFrom <= :endDate " +
           "AND r.leaveTo >= :startDate")
    List<Request> findApprovedLeaveRequestsByEmployeeIdsAndDateRange(
            @Param("employeeIds") List<Long> employeeIds,
            @Param("startDate") java.time.LocalDate startDate,
            @Param("endDate") java.time.LocalDate endDate);

    @Query("SELECT r FROM Request r " +
           "WHERE r.id = :id " +
           "AND r.employee.department.id = :departmentId " +
           "AND r.leaveType IS NOT NULL")
    java.util.Optional<Request> findByIdAndEmployeeDepartmentIdAndLeaveTypeIsNotNull(
            @Param("id") Long id,
            @Param("departmentId") Long departmentId);

    // Count methods for Quick Stats
    @Query("SELECT COUNT(r) FROM Request r JOIN r.requestType rt WHERE rt.category = :category")
    long countByRequestTypeCategory(@Param("category") String category);

    @Query("SELECT COUNT(r) FROM Request r JOIN r.requestType rt " +
           "WHERE r.status = :status AND rt.category = :category")
    long countByStatusAndRequestTypeCategory(@Param("status") String status, @Param("category") String category);

    @Query("SELECT COUNT(r) FROM Request r JOIN r.requestType rt " +
           "WHERE r.status = :status AND r.step = 'WAITING_HR' AND rt.category = :category")
    long countByStatusAndStepWaitingHRAndRequestTypeCategory(@Param("status") String status, @Param("category") String category);

    // ── Filtered Leave History (server-side) ──

    @Query(value = "SELECT r FROM Request r JOIN r.employee e JOIN e.user u " +
            "LEFT JOIN e.department d JOIN r.requestType rt " +
            "WHERE r.status <> 'PENDING' AND rt.category = 'ATTENDANCE' " +
            "AND (:status IS NULL OR r.status = :status) " +
            "AND (:departmentId IS NULL OR d.id = :departmentId) " +
            "AND (:leaveType IS NULL OR rt.code = :leaveType) " +
            "AND (:search IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "     OR LOWER(e.employeeCode) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:dateFrom IS NULL OR r.leaveFrom >= :dateFrom) " +
            "AND (:dateTo IS NULL OR r.leaveTo <= :dateTo) " +
            "ORDER BY r.updatedAt DESC, r.createdAt DESC",
            countQuery = "SELECT COUNT(r) FROM Request r JOIN r.employee e JOIN e.user u " +
            "LEFT JOIN e.department d JOIN r.requestType rt " +
            "WHERE r.status <> 'PENDING' AND rt.category = 'ATTENDANCE' " +
            "AND (:status IS NULL OR r.status = :status) " +
            "AND (:departmentId IS NULL OR d.id = :departmentId) " +
            "AND (:leaveType IS NULL OR rt.code = :leaveType) " +
            "AND (:search IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "     OR LOWER(e.employeeCode) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:dateFrom IS NULL OR r.leaveFrom >= :dateFrom) " +
            "AND (:dateTo IS NULL OR r.leaveTo <= :dateTo)")
    Page<Request> findLeaveHistoryFiltered(
            @Param("status") String status,
            @Param("departmentId") Long departmentId,
            @Param("leaveType") String leaveType,
            @Param("search") String search,
            @Param("dateFrom") java.time.LocalDate dateFrom,
            @Param("dateTo") java.time.LocalDate dateTo,
            Pageable pageable);

    // ── Calendar Events: approved/pending leaves overlapping a date range ──

    @Query("SELECT r FROM Request r JOIN FETCH r.employee e JOIN FETCH e.user u " +
            "LEFT JOIN FETCH e.department d JOIN FETCH r.requestType rt " +
            "WHERE rt.category = 'ATTENDANCE' " +
            "AND r.status IN ('APPROVED', 'PENDING') " +
            "AND r.leaveFrom IS NOT NULL AND r.leaveTo IS NOT NULL " +
            "AND r.leaveFrom <= :endDate AND r.leaveTo >= :startDate " +
            "AND (:departmentId IS NULL OR d.id = :departmentId) " +
            "AND (:leaveType IS NULL OR rt.code = :leaveType) " +
            "AND (:search IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(e.employeeCode) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Request> findCalendarEvents(
            @Param("startDate") java.time.LocalDate startDate,
            @Param("endDate") java.time.LocalDate endDate,
            @Param("departmentId") Long departmentId,
            @Param("leaveType") String leaveType,
            @Param("search") String search);

    // ── Leave Statistics ──

    @Query("SELECT COUNT(r) FROM Request r JOIN r.requestType rt " +
            "WHERE rt.category = 'ATTENDANCE' AND r.status = :status " +
            "AND r.updatedAt >= :since AND r.updatedAt <= :until")
    long countLeaveByStatusBetween(
            @Param("status") String status,
            @Param("since") LocalDateTime since,
            @Param("until") LocalDateTime until);

    @Query(value = "SELECT rt.code, COUNT(r) as cnt FROM Request r JOIN r.requestType rt " +
            "WHERE rt.category = 'ATTENDANCE' AND r.status <> 'PENDING' " +
            "GROUP BY rt.code ORDER BY cnt DESC")
    List<Object[]> findTopLeaveTypes();

    @Query(value = "SELECT AVG(TIMESTAMPDIFF(HOUR, r.created_at, r.updated_at)) FROM requests r " +
            "JOIN request_types rt ON r.request_type_id = rt.id " +
            "WHERE rt.category = 'ATTENDANCE' AND r.status IN ('APPROVED', 'REJECTED') " +
            "AND r.updated_at >= :since AND r.updated_at <= :until", nativeQuery = true)
    Double avgProcessingHoursBetween(@Param("since") LocalDateTime since, @Param("until") LocalDateTime until);

    // ── Bulk operations ──

    @Query("SELECT r FROM Request r JOIN FETCH r.employee e JOIN FETCH e.user u " +
            "LEFT JOIN FETCH e.department d JOIN FETCH r.requestType rt " +
            "WHERE r.id IN :ids AND r.status = 'PENDING' AND r.step = 'WAITING_HR' AND rt.category = 'ATTENDANCE'")
    List<Request> findPendingLeavesByIds(@Param("ids") List<Long> ids);

    // ── CSV export ──

    @Query("SELECT r FROM Request r JOIN FETCH r.employee e JOIN FETCH e.user u " +
            "LEFT JOIN FETCH e.department d JOIN FETCH r.requestType rt " +
            "WHERE r.status <> 'PENDING' AND rt.category = 'ATTENDANCE' " +
            "AND (:status IS NULL OR r.status = :status) " +
            "AND (:departmentId IS NULL OR d.id = :departmentId) " +
            "AND (cast(:startDate as timestamp) IS NULL OR r.updatedAt >= :startDate) " +
            "AND (cast(:endDate as timestamp) IS NULL OR r.updatedAt <= :endDate) " +
            "ORDER BY r.updatedAt DESC")
    List<Request> findLeaveHistoryForExport(
            @Param("status") String status,
            @Param("departmentId") Long departmentId,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate);

    // ══════════════════════════════════════════════════════════════════
    // HR Workflow Requests — Pending (non-ATTENDANCE handled here)
    // ══════════════════════════════════════════════════════════════════

    @Query("SELECT r FROM Request r JOIN FETCH r.employee e JOIN FETCH e.user u " +
            "LEFT JOIN FETCH e.department d JOIN FETCH r.requestType rt " +
            "WHERE r.status = 'PENDING' AND r.step = 'WAITING_HR' AND rt.category <> 'ATTENDANCE' " +
            "ORDER BY r.createdAt DESC")
    List<Request> findPendingWorkflowRequests();

    @Query("SELECT r FROM Request r JOIN FETCH r.employee e JOIN FETCH e.user u " +
            "LEFT JOIN FETCH e.department d JOIN FETCH r.requestType rt " +
            "WHERE r.status = 'PENDING' AND r.step = 'WAITING_HRM' AND rt.category <> 'ATTENDANCE' " +
            "ORDER BY r.createdAt DESC")
    List<Request> findHrmPendingWorkflowRequests();

    // ── HR Workflow Requests — Filtered History ──

    @Query(value = "SELECT r FROM Request r JOIN r.employee e JOIN e.user u " +
            "LEFT JOIN e.department d JOIN r.requestType rt " +
            "WHERE r.status <> 'PENDING' AND rt.category <> 'ATTENDANCE' " +
            "AND (:status IS NULL OR r.status = :status) " +
            "AND (:categoryCode IS NULL OR rt.category = :categoryCode) " +
            "AND (:search IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "     OR LOWER(e.employeeCode) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "     OR LOWER(r.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:dateFrom IS NULL OR r.createdAt >= :dateFrom) " +
            "AND (:dateTo IS NULL OR r.createdAt <= :dateTo) " +
            "ORDER BY r.updatedAt DESC, r.createdAt DESC",
            countQuery = "SELECT COUNT(r) FROM Request r JOIN r.employee e JOIN e.user u " +
            "LEFT JOIN e.department d JOIN r.requestType rt " +
            "WHERE r.status <> 'PENDING' AND rt.category <> 'ATTENDANCE' " +
            "AND (:status IS NULL OR r.status = :status) " +
            "AND (:categoryCode IS NULL OR rt.category = :categoryCode) " +
            "AND (:search IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "     OR LOWER(e.employeeCode) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "     OR LOWER(r.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:dateFrom IS NULL OR r.createdAt >= :dateFrom) " +
            "AND (:dateTo IS NULL OR r.createdAt <= :dateTo)")
    Page<Request> findWorkflowRequestsFiltered(
            @Param("status") String status,
            @Param("categoryCode") String categoryCode,
            @Param("search") String search,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            Pageable pageable);

    // ── HR Workflow Requests — Stats ──

    @Query("SELECT COUNT(r) FROM Request r JOIN r.requestType rt " +
            "WHERE rt.category <> 'ATTENDANCE' AND r.status = :status " +
            "AND r.updatedAt >= :since")
    long countWorkflowByStatusSince(
            @Param("status") String status,
            @Param("since") LocalDateTime since);

    @Query(value = "SELECT AVG(TIMESTAMPDIFF(HOUR, r.created_at, r.updated_at)) FROM requests r " +
            "JOIN request_types rt ON r.request_type_id = rt.id " +
            "WHERE rt.category <> 'ATTENDANCE' AND r.status IN ('APPROVED', 'REJECTED') " +
            "AND r.updated_at >= :since", nativeQuery = true)
    Double avgWorkflowProcessingHoursSince(@Param("since") LocalDateTime since);

    @Query(value = "SELECT rt.name, COUNT(r) as cnt FROM Request r JOIN r.requestType rt " +
            "WHERE rt.category <> 'ATTENDANCE' AND r.status <> 'PENDING' " +
            "GROUP BY rt.name ORDER BY cnt DESC")
    List<Object[]> findTopWorkflowTypes();

    @Query("SELECT COUNT(r) FROM Request r JOIN r.requestType rt " +
            "WHERE r.status = 'PENDING' AND r.step = 'WAITING_HR' AND rt.category <> 'ATTENDANCE'")
    long countPendingWorkflowRequests();

    // ── HR Workflow Requests — Bulk operations ──

    @Query("SELECT r FROM Request r JOIN FETCH r.employee e JOIN FETCH e.user u " +
            "LEFT JOIN FETCH e.department d JOIN FETCH r.requestType rt " +
            "WHERE r.id IN :ids AND r.status = 'PENDING' AND r.step = 'WAITING_HR' AND rt.category <> 'ATTENDANCE'")
    List<Request> findPendingWorkflowRequestsByIds(@Param("ids") List<Long> ids);

    // ══════════════════════════════════════════════════════════════════════════
    // CRITICAL REQUESTS - For Email Notification
    // ══════════════════════════════════════════════════════════════════════════
    
    /**
     * Find all requests by status and priority
     * Used for finding CRITICAL pending requests for email notifications
     */
    @Query("SELECT r FROM Request r " +
           "JOIN FETCH r.employee e " +
           "JOIN FETCH e.user u " +
           "LEFT JOIN FETCH e.position p " +
           "WHERE r.status = :status AND r.priority = :priority " +
           "ORDER BY r.createdAt DESC")
    List<Request> findByStatusAndPriorityOrderByCreatedAtDesc(
            @Param("status") String status, 
            @Param("priority") String priority);
}
