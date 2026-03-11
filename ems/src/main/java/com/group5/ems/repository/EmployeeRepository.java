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

    @Query("select e from Employee  e join e.user u where u.status = :status")
    List<Employee> findByStatus(@Param("status") String status);

    @Query("select count(e) from Employee e where month(e.hireDate) = month (current_date ) and year(e.hireDate) = year(current_date )")
    long newThisMonth();
    @Query("select count(e) from Employee e where year(e.hireDate) = year(current_date )")
    long newThisYear();

    int countByDepartmentId(Long id);
}

