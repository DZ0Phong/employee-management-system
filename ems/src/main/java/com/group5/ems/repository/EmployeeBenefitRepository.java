package com.group5.ems.repository;

import com.group5.ems.entity.EmployeeBenefit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface EmployeeBenefitRepository extends JpaRepository<EmployeeBenefit, Long> {

    List<EmployeeBenefit> findByEmployeeId(Long employeeId);

    List<EmployeeBenefit> findByEmployeeIdAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqual(
            Long employeeId, LocalDate to, LocalDate from);
}

