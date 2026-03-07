package com.group5.ems.repository;

import com.group5.ems.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractRepository extends JpaRepository<Contract, Long> {

    List<Contract> findByEmployeeId(Long employeeId);

    List<Contract> findByEmployeeIdAndStatus(Long employeeId, String status);
}

