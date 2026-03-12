package com.group5.ems.repository;

import com.group5.ems.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
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

    @Query("select d.name, count(e) from Employee e join e.department d group by d.name order by count(e) desc")
    List<Object []> countEmployeeByDepartmentName();
    @Query("SELECT COUNT(e) FROM Employee e WHERE e.status = :status")
    Long countByStatus(@Param("status") String status);

    @Query("SELECT AVG(DATEDIFF(CURRENT_DATE, e.hireDate)) FROM Employee e WHERE e.status = 'ACTIVE' AND e.hireDate IS NOT NULL")
    Double getAverageTenureInDays();

    @Query(value = "SELECT e FROM Employee e JOIN e.user u JOIN e.department d JOIN e.position p " +
            "WHERE (:search IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%',:search,'%')) " +
            "OR LOWER(e.employeeCode) LIKE LOWER(CONCAT('%',:search,'%')) " +
            "OR LOWER(u.email) LIKE LOWER(CONCAT('%',:search,'%'))) " +
            "AND (:department IS NULL OR d.name = :department) " +
            "AND (:status IS NULL OR e.status = :status)",
            countQuery = "SELECT COUNT(e) FROM Employee e JOIN e.user u JOIN e.department d JOIN e.position p " +
                    "WHERE (:search IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%',:search,'%')) " +
                    "OR LOWER(e.employeeCode) LIKE LOWER(CONCAT('%',:search,'%')) " +
                    "OR LOWER(u.email) LIKE LOWER(CONCAT('%',:search,'%'))) " +
                    "AND (:department IS NULL OR d.name = :department) " +
                    "AND (:status IS NULL OR e.status = :status)")
    Page<Employee> searchEmployees(@Param("search") String search,
                                   @Param("department") String department,
                                   @Param("status") String status,
                                   Pageable pageable);

    @Query("SELECT e FROM Employee e LEFT JOIN FETCH e.user LEFT JOIN FETCH e.department LEFT JOIN FETCH e.position WHERE e.id = :id")
    Optional<Employee> findByIdWithDetails(@Param("id") Long id);

    @Query("select count (e) from Employee e where e.hireDate <= :date")
    long hiredDateUpTo(@Param("date") LocalDate localDate);

    @Query("select count(e) from Employee  e join e.user u where e.hireDate <= :date and u.status = :status")
    long countHireUpToByStatus(@Param("date") LocalDate localDate,@Param("status") String status);

}
