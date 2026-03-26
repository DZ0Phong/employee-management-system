package com.group5.ems.repository;

import com.group5.ems.entity.EmployeeLeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmployeeLeaveBalanceRepository extends JpaRepository<EmployeeLeaveBalance, Long> {
    
    @Query("SELECT elb FROM EmployeeLeaveBalance elb WHERE elb.employee.id = :employeeId AND elb.year = :year")
    Optional<EmployeeLeaveBalance> findByEmployeeIdAndYear(@Param("employeeId") Long employeeId, @Param("year") Integer year);
}
