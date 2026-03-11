package com.group5.ems.repository;

import com.group5.ems.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmployeeCode(String employeeCode);

    Optional<Employee> findByUserId(Long userId);

    List<Employee> findByDepartmentId(Long departmentId);

    @Query("select e from Employee e join fetch e.user u where e.departmentId = :departmentId")
    List<Employee> findByDepartmentIdWithUser(@Param("departmentId") Long departmentId);

    List<Employee> findByStatus(String status);

    int countByDepartmentId(Long id);

    int countByStatus(String status);
}

