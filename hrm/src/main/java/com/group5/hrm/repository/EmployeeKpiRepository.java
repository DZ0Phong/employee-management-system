package com.group5.hrm.repository;

import com.group5.hrm.entity.EmployeeKpi;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeKpiRepository extends JpaRepository<EmployeeKpi, Long> {

    List<EmployeeKpi> findByEmployeeId(Long employeeId);

    List<EmployeeKpi> findByEmployeeIdAndPeriod(Long employeeId, String period);
}
