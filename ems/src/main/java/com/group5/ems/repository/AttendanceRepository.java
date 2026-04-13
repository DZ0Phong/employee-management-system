package com.group5.ems.repository;

import com.group5.ems.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByEmployeeIdAndWorkDate(Long employeeId, LocalDate workDate);

    List<Attendance> findByEmployeeIdAndWorkDateBetween(Long employeeId, LocalDate from, LocalDate to);

    List<Attendance> findByWorkDate(LocalDate workDate);

    List<Attendance> findByEmployeeIdAndWorkDateBetweenOrderByWorkDateDesc(Long employeeId, LocalDate from, LocalDate to);

    int countByWorkDate(LocalDate workDate);

    int countByWorkDateAndStatus(LocalDate workDate, String status);

    List<Attendance> findByEmployeeIdInAndWorkDateBetweenOrderByWorkDateAsc(List<Long> employeeIds, LocalDate from, LocalDate to);

    @org.springframework.data.jpa.repository.Query("SELECT new com.group5.ems.dto.response.HrAttendanceDetailDTO(" +
            "a.id, e.employeeCode, u.fullName, d.name, a.workDate, a.checkIn, a.checkOut, a.status, a.note) " +
            "FROM Attendance a " +
            "JOIN a.employee e " +
            "JOIN e.user u " +
            "LEFT JOIN e.department d " +
            "WHERE a.workDate = :workDate " +
            "AND (:departmentId IS NULL OR d.id = :departmentId) " +
            "AND (:status IS NULL OR :status = '' OR a.status = :status) " +
            "AND (:search IS NULL OR :search = '' OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(e.employeeCode) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY a.checkIn ASC, u.fullName ASC")
    org.springframework.data.domain.Page<com.group5.ems.dto.response.HrAttendanceDetailDTO> findAttendanceDetails(
            @org.springframework.data.repository.query.Param("workDate") LocalDate workDate,
            @org.springframework.data.repository.query.Param("departmentId") Long departmentId,
            @org.springframework.data.repository.query.Param("status") String status,
            @org.springframework.data.repository.query.Param("search") String search,
            org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT new com.group5.ems.dto.response.HrAttendanceDetailDTO(" +
            "a.id, e.employeeCode, u.fullName, d.name, a.workDate, a.checkIn, a.checkOut, a.status, a.note) " +
            "FROM Attendance a " +
            "JOIN a.employee e " +
            "JOIN e.user u " +
            "LEFT JOIN e.department d " +
            "WHERE a.workDate = :workDate " +
            "AND (:departmentId IS NULL OR d.id = :departmentId) " +
            "AND (:status IS NULL OR :status = '' OR a.status = :status) " +
            "AND (:search IS NULL OR :search = '' OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(e.employeeCode) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY a.checkIn ASC, u.fullName ASC")
    java.util.List<com.group5.ems.dto.response.HrAttendanceDetailDTO> findAllAttendanceDetails(
            @org.springframework.data.repository.query.Param("workDate") LocalDate workDate,
            @org.springframework.data.repository.query.Param("departmentId") Long departmentId,
            @org.springframework.data.repository.query.Param("status") String status,
            @org.springframework.data.repository.query.Param("search") String search);

    // ── HR Reports: Aggregation Queries (read-only) ──────────────────────────

    @org.springframework.data.jpa.repository.Query(
            "SELECT COUNT(a) FROM Attendance a WHERE a.status = :status " +
            "AND a.workDate >= :from AND a.workDate <= :to")
    long countByStatusAndWorkDateBetween(
            @org.springframework.data.repository.query.Param("status") String status,
            @org.springframework.data.repository.query.Param("from") LocalDate from,
            @org.springframework.data.repository.query.Param("to") LocalDate to);

    @org.springframework.data.jpa.repository.Query(
            "SELECT COUNT(a) FROM Attendance a WHERE a.workDate >= :from AND a.workDate <= :to")
    long countByWorkDateBetween(
            @org.springframework.data.repository.query.Param("from") LocalDate from,
            @org.springframework.data.repository.query.Param("to") LocalDate to);
}
