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
    List<Payslip> findByStatus(String status);
    
    List<Payslip> findByStatusOrderByIdDesc(String status);

    List<Payslip> findByEmployeeIdOrderByIdDesc(Long employeeId);
    Optional<Payslip> findTopByEmployeeIdOrderByIdDesc(Long employeeId);
    
    @Query("SELECT COUNT(p) FROM Payslip p WHERE p.status = :status")
    Long countByStatus(@Param("status") String status);
    
    @Query("SELECT p FROM Payslip p " +
           "JOIN FETCH p.employee e " +
           "JOIN FETCH e.user u " +
           "LEFT JOIN FETCH e.position pos " +
           "LEFT JOIN FETCH p.period per " +
           "WHERE p.status = :status " +
           "ORDER BY p.id DESC")
    Page<Payslip> findPayslipsByStatus(@Param("status") String status, Pageable pageable);

    @Query("SELECT p FROM Payslip p " +
           "JOIN FETCH p.employee e " +
           "JOIN FETCH e.user u " +
           "LEFT JOIN FETCH e.position pos " +
           "LEFT JOIN FETCH p.period per " +
           "ORDER BY p.id DESC")
    Page<Payslip> findAllPayslips(Pageable pageable);
}
