package com.group5.ems.repository;

import com.group5.ems.entity.Salary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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
}

