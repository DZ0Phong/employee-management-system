package com.group5.ems.repository;

import com.group5.ems.entity.Payslip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BankExportRepository extends JpaRepository<Payslip, Long> {

    /**
     * Optimized query to fetch required data for Bank Export CSV.
     * Joins Payslip, Employee, User (for full name), and EmployeeBankDetail.
     * Returns a list of Object arrays where [0] is Payslip and [1] is EmployeeBankDetail.
     */
    @Query("SELECT p, b FROM Payslip p " +
           "JOIN FETCH p.employee e " +
           "JOIN FETCH e.user u " +
           "JOIN EmployeeBankDetail b ON b.employee.id = e.id " +
           "WHERE p.periodId = :periodId " +
           "AND p.status = 'APPROVED' " +
           "AND b.isPrimary = true")
    List<Object[]> findApprovedPayslipsWithPrimaryBankDetails(@Param("periodId") Long periodId);
}
