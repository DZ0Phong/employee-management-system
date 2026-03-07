package com.group5.ems.repository;

import com.group5.ems.entity.EmployeeSkill;
import com.group5.ems.entity.EmployeeSkillId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeSkillRepository extends JpaRepository<EmployeeSkill, EmployeeSkillId> {

    List<EmployeeSkill> findByEmployeeId(Long employeeId);

    List<EmployeeSkill> findBySkillId(Long skillId);
}
