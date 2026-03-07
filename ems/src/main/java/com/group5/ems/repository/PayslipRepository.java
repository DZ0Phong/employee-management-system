package com.group5.ems.repository;

import com.group5.ems.entity.Payslip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PayslipRepository extends JpaRepository<Payslip, Long> {

    List<Payslip> findByEmployeeId(Long employeeId);

    List<Payslip> findByPeriodId(Long periodId);

    Optional<Payslip> findByEmployeeIdAndPeriodId(Long employeeId, Long periodId);
}
