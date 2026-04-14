package com.group5.ems.repository;

import com.group5.ems.entity.EmployeeSkill;
import com.group5.ems.entity.EmployeeSkillId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeSkillRepository extends JpaRepository<EmployeeSkill, EmployeeSkillId> {

    List<EmployeeSkill> findByEmployeeId(Long employeeId);

    List<EmployeeSkill> findBySkillId(Long skillId);

    @org.springframework.data.jpa.repository.Query("SELECT es FROM EmployeeSkill es JOIN FETCH es.skill WHERE es.employeeId IN :employeeIds")
    List<EmployeeSkill> findByEmployeeIdInWithSkill(@org.springframework.data.repository.query.Param("employeeIds") List<Long> employeeIds);
}
