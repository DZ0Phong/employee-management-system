package com.group5.ems.repository;

import com.group5.ems.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    Optional<Department> findByCode(String code);

    List<Department> findByParentIdIsNull();

    List<Department> findByParentId(Long parentId);

    List<Department> findByManagerId(Long managerId);
}

