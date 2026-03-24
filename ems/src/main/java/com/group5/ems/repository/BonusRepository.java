package com.group5.ems.repository;

import com.group5.ems.entity.Bonus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface BonusRepository extends JpaRepository<Bonus, Long> {

    List<Bonus> findByEmployeeId(Long employeeId);

    List<Bonus> findByEmployeeIdAndStatus(Long employeeId, String status);

    /**
     * Sums approved bonus amounts for an employee within a date range.
     */
    @Query("SELECT COALESCE(SUM(b.amount), 0) FROM Bonus b " +
           "WHERE b.employeeId = :empId AND b.status = 'APPROVED' " +
           "AND b.bonusDate BETWEEN :startDate AND :endDate")
    BigDecimal sumApprovedBonuses(@Param("empId") Long empId,
                                  @Param("startDate") LocalDate startDate,
                                  @Param("endDate") LocalDate endDate);
}

