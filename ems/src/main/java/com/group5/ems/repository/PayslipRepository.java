package com.group5.ems.repository;

import com.group5.ems.entity.Payslip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

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
}
