package com.group5.ems.repository;

import com.group5.ems.entity.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.group5.ems.entity.Department;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    Optional<Department> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    List<Department> findByParentIdIsNull();

    List<Department> findByParentId(Long parentId);

    List<Department> findByManagerId(Long managerId);

    Page<Department> findByNameContainingIgnoreCaseOrCodeIgnoreCase(String keyword, String code, Pageable pageable);

    @Query("select count(distinct d.parentId) from Department d where d.parentId is not null")
    long countAllParentId();


}

