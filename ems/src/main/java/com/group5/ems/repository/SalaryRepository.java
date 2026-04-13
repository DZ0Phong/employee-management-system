package com.group5.ems.repository;

import com.group5.ems.entity.Salary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SalaryRepository extends JpaRepository<Salary, Long> {

    List<Salary> findByEmployeeId(Long employeeId);

    Optional<Salary> findFirstByEmployeeIdAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqual(
            Long employeeId, LocalDate date, LocalDate dateTo);

    Optional<Salary> findFirstByEmployeeIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
            Long employeeId, LocalDate date);

    List<Salary> findByEmployeeIdOrderByEffectiveFromDesc(Long employeeId);
    Optional<Salary> findTopByEmployeeIdOrderByEffectiveFromDesc(Long employeeId);

    @Query("SELECT s FROM Salary s " +
           "WHERE s.employeeId IN :employeeIds " +
           "AND s.effectiveFrom = (" +
           "    SELECT MAX(s2.effectiveFrom) FROM Salary s2 WHERE s2.employeeId = s.employeeId" +
           ")")
    List<Salary> findLatestByEmployeeIds(@Param("employeeIds") List<Long> employeeIds);
    
    // ── Analytics Queries ──────────────────────────────────────────────────────
    
    @Query("SELECT " +
            "CASE " +
            "  WHEN s.baseAmount < 50000 THEN '<50k' " +
            "  WHEN s.baseAmount < 80000 THEN '50-80k' " +
            "  WHEN s.baseAmount < 110000 THEN '80-110k' " +
            "  WHEN s.baseAmount < 150000 THEN '110-150k' " +
            "  ELSE '150k+' " +
            "END, " +
            "COUNT(DISTINCT s.employeeId) " +
            "FROM Salary s " +
            "WHERE s.effectiveTo IS NULL OR s.effectiveTo >= CURRENT_DATE " +
            "GROUP BY CASE " +
            "  WHEN s.baseAmount < 50000 THEN '<50k' " +
            "  WHEN s.baseAmount < 80000 THEN '50-80k' " +
            "  WHEN s.baseAmount < 110000 THEN '80-110k' " +
            "  WHEN s.baseAmount < 150000 THEN '110-150k' " +
            "  ELSE '150k+' " +
            "END")
    List<Object[]> countBySalaryBand();
    
    @Query("SELECT AVG(s.baseAmount) FROM Salary s " +
            "WHERE s.effectiveTo IS NULL OR s.effectiveTo >= CURRENT_DATE")
    Double getAverageSalary();

    // ── HR Reports: Aggregation Queries (read-only) ──────────────────────────

    @Query("SELECT COALESCE(SUM(s.baseAmount), 0) FROM Salary s " +
           "WHERE s.effectiveTo IS NULL OR s.effectiveTo >= CURRENT_DATE")
    java.math.BigDecimal sumCurrentPayrollCost();

    @Query("SELECT d.name, AVG(s.baseAmount) FROM Salary s " +
           "JOIN s.employee e JOIN e.department d " +
           "WHERE s.effectiveTo IS NULL OR s.effectiveTo >= CURRENT_DATE " +
           "GROUP BY d.name ORDER BY AVG(s.baseAmount) DESC")
    List<Object[]> avgSalaryByDepartment();

    /**
     * Get average salary at a specific date
     */
    @Query("SELECT AVG(s.baseAmount) FROM Salary s " +
           "WHERE s.effectiveFrom <= :date " +
           "AND (s.effectiveTo IS NULL OR s.effectiveTo >= :date)")
    Double getAverageSalaryAtDate(@Param("date") LocalDate date);
}
