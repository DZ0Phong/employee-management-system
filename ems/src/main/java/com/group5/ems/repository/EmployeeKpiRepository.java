package com.group5.ems.repository;

import com.group5.ems.entity.EmployeeKpi;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeKpiRepository extends JpaRepository<EmployeeKpi, Long> {

    List<EmployeeKpi> findByEmployeeId(Long employeeId);

    List<EmployeeKpi> findByEmployeeIdAndPeriod(Long employeeId, String period);
}
