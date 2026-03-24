package com.group5.ems.repository;

import com.group5.ems.entity.RewardDiscipline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface RewardDisciplineRepository extends JpaRepository<RewardDiscipline, Long> {

    List<RewardDiscipline> findByEmployeeId(Long employeeId);

    List<RewardDiscipline> findByEmployeeIdAndRecordType(Long employeeId, String recordType);

    /**
     * Sums discipline deduction amounts for an employee within a date range.
     */
    @Query("SELECT COALESCE(SUM(rd.amount), 0) FROM RewardDiscipline rd " +
           "WHERE rd.employeeId = :empId AND rd.recordType = 'DISCIPLINE' " +
           "AND rd.decisionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumDeductions(@Param("empId") Long empId,
                             @Param("startDate") LocalDate startDate,
                             @Param("endDate") LocalDate endDate);
}
