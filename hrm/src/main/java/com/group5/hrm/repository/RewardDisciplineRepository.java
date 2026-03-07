package com.group5.hrm.repository;

import com.group5.hrm.entity.RewardDiscipline;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RewardDisciplineRepository extends JpaRepository<RewardDiscipline, Long> {

    List<RewardDiscipline> findByEmployeeId(Long employeeId);

    List<RewardDiscipline> findByEmployeeIdAndRecordType(Long employeeId, String recordType);
}
