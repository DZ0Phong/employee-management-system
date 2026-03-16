package com.group5.ems.repository;

import com.group5.ems.entity.Salary;
import org.springframework.data.jpa.repository.JpaRepository;

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
}

