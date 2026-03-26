package com.group5.ems.repository;

import com.group5.ems.entity.Payslip;
import com.group5.ems.dto.hr.PayslipReviewDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

public interface PayslipRepository extends JpaRepository<Payslip, Long> {

    List<Payslip> findByEmployeeId(Long employeeId);

    List<Payslip> findByPeriodId(Long periodId);

    Optional<Payslip> findByEmployeeIdAndPeriodId(Long employeeId, Long periodId);

    // Methods for Payroll Approval

    List<Payslip> findByStatusOrderByIdDesc(String status);

    List<Payslip> findByEmployeeIdOrderByIdDesc(Long employeeId);
    Optional<Payslip> findTopByEmployeeIdOrderByIdDesc(Long employeeId);

    // Lấy payslips theo status, kèm employee và period
    @Query("SELECT p FROM Payslip p " +
            "JOIN FETCH p.employee e " +
            "JOIN FETCH e.user u " +
            "JOIN FETCH e.department d " +
            "JOIN FETCH p.period " +
            "WHERE p.status = :status")
    List<Payslip> findByStatus(@Param("status") String status);

    // Lấy payslips theo department và status
    @Query("SELECT p FROM Payslip p " +
            "JOIN FETCH p.employee e " +
            "JOIN FETCH e.department d " +
            "WHERE e.departmentId = :deptId AND p.status = :status")
    List<Payslip> findByDepartmentAndStatus(@Param("deptId") Long deptId,
                                            @Param("status") String status);

    // Approve tất cả payslips của 1 department
    @Query("UPDATE Payslip p SET p.status = 'APPROVED', p.approvedBy = :approverId " +
            "WHERE p.employeeId IN (SELECT e.id FROM Employee e WHERE e.departmentId = :deptId) " +
            "AND p.status = 'PENDING'")
    @org.springframework.data.jpa.repository.Modifying
    int approveByDepartment(@Param("deptId") Long deptId,
                            @Param("approverId") Long approverId);

    // --- Task 4.1: Review Dashboard Queries ---

    @Query("SELECT new com.group5.ems.dto.hr.PayslipReviewDTO(" +
            "p.id, e.employeeCode, u.fullName, p.actualBaseSalary, p.totalGrossSalary, p.totalDeduction, p.netSalary, p.status, p.totalOtAmount, p.totalBonus) " +
            "FROM Payslip p " +
            "JOIN p.employee e " +
            "JOIN e.user u " +
            "WHERE p.periodId = :periodId")
    Page<PayslipReviewDTO> findReviewDTOByPeriodId(@Param("periodId") Long periodId, Pageable pageable);

    @Query("SELECT new com.group5.ems.dto.hr.PayslipReviewDTO(" +
            "p.id, e.employeeCode, u.fullName, p.actualBaseSalary, p.totalGrossSalary, p.totalDeduction, p.netSalary, p.status, p.totalOtAmount, p.totalBonus) " +
            "FROM Payslip p " +
            "JOIN p.employee e " +
            "JOIN e.user u " +
            "WHERE p.periodId = :periodId AND p.netSalary <= 0")
    Page<PayslipReviewDTO> findReviewDTONegativeNetByPeriodId(@Param("periodId") Long periodId, Pageable pageable);

    @Query("SELECT new com.group5.ems.dto.hr.PayslipReviewDTO(" +
            "p.id, e.employeeCode, u.fullName, p.actualBaseSalary, p.totalGrossSalary, p.totalDeduction, p.netSalary, p.status, p.totalOtAmount, p.totalBonus) " +
            "FROM Payslip p " +
            "JOIN p.employee e " +
            "JOIN e.user u " +
            "WHERE p.periodId = :periodId AND p.totalOtAmount > :threshold")
    Page<PayslipReviewDTO> findReviewDTOHighOvertimeByPeriodId(@Param("periodId") Long periodId, @Param("threshold") BigDecimal threshold, Pageable pageable);

    @Query("SELECT new com.group5.ems.dto.hr.PayslipReviewDTO(" +
            "p.id, e.employeeCode, u.fullName, p.actualBaseSalary, p.totalGrossSalary, p.totalDeduction, p.netSalary, p.status, p.totalOtAmount, p.totalBonus) " +
            "FROM Payslip p " +
            "JOIN p.employee e " +
            "JOIN e.user u " +
            "WHERE p.periodId = :periodId AND (p.netSalary <= 0 OR p.totalOtAmount > :threshold)")
    Page<PayslipReviewDTO> findReviewDTOAnomaliesByPeriodId(@Param("periodId") Long periodId, @Param("threshold") BigDecimal threshold, Pageable pageable);

    @Query("SELECT COUNT(p) FROM Payslip p WHERE p.periodId = :periodId AND p.netSalary <= 0")
    int countNegativeNetByPeriodId(@Param("periodId") Long periodId);

    @Query("SELECT COUNT(p) FROM Payslip p WHERE p.periodId = :periodId AND p.totalOtAmount > :threshold")
    int countHighOvertimeByPeriodId(@Param("periodId") Long periodId, @Param("threshold") BigDecimal threshold);

    @Query("SELECT COUNT(p) FROM Payslip p WHERE p.periodId = :periodId AND (p.netSalary <= 0 OR p.totalOtAmount > :threshold)")
    int countAnomaliesByPeriodId(@Param("periodId") Long periodId, @Param("threshold") BigDecimal threshold);

    @Query("SELECT COUNT(p) FROM Payslip p WHERE p.periodId = :periodId AND p.status = 'PENDING'")
    long countPendingByPeriodId(@Param("periodId") Long periodId);

    @Transactional
    @Modifying
    @Query("UPDATE Payslip p SET p.status = 'APPROVED', p.approvedBy = :approverId " +
            "WHERE p.periodId = :periodId AND p.status = 'PENDING'")
    int approveAllPendingInPeriod(@Param("periodId") Long periodId, @Param("approverId") Long approverId);

    @Query("SELECT SUM(p.totalGrossSalary) FROM Payslip p WHERE p.periodId = :periodId")
    BigDecimal sumTotalGrossByPeriodId(@Param("periodId") Long periodId);

    @Query("SELECT SUM(p.netSalary) FROM Payslip p WHERE p.periodId = :periodId")
    BigDecimal sumTotalNetByPeriodId(@Param("periodId") Long periodId);

    @Query("SELECT COUNT(p) FROM Payslip p WHERE p.periodId = :periodId")
    int countByPeriodId(@Param("periodId") Long periodId);
}
