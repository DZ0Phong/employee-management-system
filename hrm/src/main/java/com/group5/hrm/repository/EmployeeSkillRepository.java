package com.group5.hrm.repository;

import com.group5.hrm.entity.EmployeeSkill;
import com.group5.hrm.entity.EmployeeSkillId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeSkillRepository extends JpaRepository<EmployeeSkill, EmployeeSkillId> {

    List<EmployeeSkill> findByEmployeeId(Long employeeId);

    List<EmployeeSkill> findBySkillId(Long skillId);
}
