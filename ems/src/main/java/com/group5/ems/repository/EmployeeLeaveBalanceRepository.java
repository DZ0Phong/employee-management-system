package com.group5.ems.repository;

import com.group5.ems.entity.EmployeeLeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface EmployeeLeaveBalanceRepository extends JpaRepository<EmployeeLeaveBalance, Long> {
    
    @Query("SELECT elb FROM EmployeeLeaveBalance elb WHERE elb.employee.id = :employeeId AND elb.year = :year")
    Optional<EmployeeLeaveBalance> findByEmployeeIdAndYear(@Param("employeeId") Long employeeId, @Param("year") Integer year);

    @Query("SELECT elb FROM EmployeeLeaveBalance elb JOIN FETCH elb.employee WHERE elb.year = :year")
    List<EmployeeLeaveBalance> findAllByYear(@Param("year") int year);

    @Query("SELECT COALESCE(SUM(elb.totalDays), 0) FROM EmployeeLeaveBalance elb WHERE elb.year = :year")
    BigDecimal sumTotalDaysByYear(@Param("year") int year);

    @Query("SELECT COALESCE(SUM(elb.usedDays), 0) FROM EmployeeLeaveBalance elb WHERE elb.year = :year")
    BigDecimal sumUsedDaysByYear(@Param("year") int year);

    @Query("SELECT COALESCE(SUM(elb.pendingDays), 0) FROM EmployeeLeaveBalance elb WHERE elb.year = :year")
    BigDecimal sumPendingDaysByYear(@Param("year") int year);

    @Query("SELECT COALESCE(SUM(elb.remainingDays), 0) FROM EmployeeLeaveBalance elb WHERE elb.year = :year")
    BigDecimal sumRemainingDaysByYear(@Param("year") int year);

    @Query("SELECT COUNT(elb) FROM EmployeeLeaveBalance elb WHERE elb.year = :year")
    long countByYear(@Param("year") int year);

    @Query("SELECT COUNT(elb), COALESCE(SUM(elb.totalDays), 0), COALESCE(SUM(elb.usedDays), 0), COALESCE(SUM(elb.pendingDays), 0), COALESCE(SUM(elb.remainingDays), 0) FROM EmployeeLeaveBalance elb WHERE elb.year = :year")
    List<Object[]> getAggregatedBalancesByYear(@Param("year") int year);

    @Query("SELECT elb FROM EmployeeLeaveBalance elb JOIN FETCH elb.employee e LEFT JOIN FETCH e.user u LEFT JOIN FETCH e.department d WHERE elb.year = :year " +
           "AND (:departmentId IS NULL OR e.department.id = :departmentId) " +
           "AND (:search IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(e.employeeCode) LIKE LOWER(CONCAT('%', :search, '%')))")
    org.springframework.data.domain.Page<EmployeeLeaveBalance> findBalancesFiltered(@Param("year") int year, @Param("departmentId") Long departmentId, @Param("search") String search, org.springframework.data.domain.Pageable pageable);
}
