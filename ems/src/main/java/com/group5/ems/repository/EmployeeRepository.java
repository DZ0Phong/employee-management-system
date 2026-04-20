package com.group5.ems.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.group5.ems.entity.Employee;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

    Optional<Employee> findByUserId(Long userId);

    Optional<Employee> findByEmployeeCode(String employeeCode);

    @Query("select e from Employee  e join e.user u where u.status = :status")
    List<Employee> findByStatus(@Param("status") String status);

    @Query("select distinct e from Employee e " +
            "join fetch e.user u " +
            "left join fetch e.position p " +
            "left join fetch e.department d " +
            "where e.departmentId = :departmentId")
    List<Employee> findByDepartmentIdWithUser(@Param("departmentId") Long departmentId);

    @Query("select e from Employee e join e.user u where u.status = :status")
    Page<Employee> findByStatus(@Param("status") String status, Pageable pageable);

    @Query("select count(e) from Employee e where month(e.hireDate) = month (current_date ) and year(e.hireDate) = year(current_date )")
    long newThisMonth();

    @Query("select count(e) from Employee e where year(e.hireDate) = year(current_date )")
    long newThisYear();

    int countByDepartmentId(Long id);

    @Query("select d.name, count(e) from Employee e join e.department d group by d.name order by count(e) desc")
    List<Object[]> countEmployeeByDepartmentName();

    @Query("SELECT COUNT(e) FROM Employee e WHERE e.status = :status")
    Long countByStatus(@Param("status") String status);

    @Query("SELECT AVG(DATEDIFF(CURRENT_DATE, e.hireDate)) FROM Employee e WHERE e.status = 'ACTIVE' AND e.hireDate IS NOT NULL")
    Double getAverageTenureInDays();

       @Query("SELECT AVG(DATEDIFF(:date, e.hireDate)) FROM Employee e " +
                     "WHERE e.status = 'ACTIVE' AND e.hireDate IS NOT NULL AND e.hireDate <= :date")
       Double getAverageTenureInDaysAtDate(@Param("date") LocalDate date);

       

    @Query("SELECT e FROM Employee e LEFT JOIN FETCH e.user LEFT JOIN FETCH e.department LEFT JOIN FETCH e.position WHERE e.id = :id")
    Optional<Employee> findByIdWithDetails(@Param("id") Long id);

    @Query("select count (e) from Employee e where e.hireDate <= :date")
    long hiredDateUpTo(@Param("date") LocalDate localDate);

    @Query("select count(e) from Employee  e join e.user u where e.hireDate <= :date and u.status = :status")
    long countHireUpToByStatus(@Param("date") LocalDate localDate, @Param("status") String status);

    // Thêm vào EmployeeRepository
    @Query("SELECT MONTH(e.hireDate), COUNT(e) FROM Employee e " +
            "WHERE YEAR(e.hireDate) = :year " +
            "GROUP BY MONTH(e.hireDate) ORDER BY MONTH(e.hireDate)")
    List<Object[]> countHiringByMonth(@Param("year") int year);

    @Query("SELECT e FROM Employee e " +
            "WHERE e.status IN ('ACTIVE', 'ON_LEAVE') " +
            "AND e.hireDate <= :periodEndDate")
    Page<Employee> findEligibleEmployeesForPeriod(@Param("periodEndDate") LocalDate periodEndDate,
            Pageable pageable);

    @Query("SELECT e FROM Employee e " +
            "WHERE e.status IN ('ACTIVE', 'ON_LEAVE') " +
            "AND e.hireDate <= :periodEndDate")
    List<Employee> findEligibleEmployeesForPeriodList(@Param("periodEndDate") LocalDate periodEndDate);

    @Query("""
            SELECT e FROM Employee e
            JOIN FETCH e.user u
            JOIN u.userRoles ur
            JOIN ur.role r
            WHERE r.code = 'HR'
              AND e.status = 'ACTIVE'
            """)
    List<Employee> findAllWithUser();

    @Query("SELECT DISTINCT e FROM Employee e JOIN FETCH e.user u " +
            "JOIN UserRole ur ON ur.userId = u.id " +
            "JOIN ur.role r WHERE r.code IN :roleCodes")
    List<Employee> findEmployeesByRoleCodes(@Param("roleCodes") List<String> roleCodes);

    // Methods for Dashboard KPI calculations
    @Query("SELECT COUNT(e) FROM Employee e WHERE e.status = 'ACTIVE' AND e.hireDate <= :date")
    Long countActiveEmployeesAtDate(@Param("date") LocalDate date);

    @Query("SELECT COUNT(e) FROM Employee e WHERE e.status = 'TERMINATED' " +
            "AND e.updatedAt >= :startDateTime AND e.updatedAt <= :endDateTime")
    Long countTerminatedInPeriod(@Param("startDateTime") java.time.LocalDateTime startDateTime,
            @Param("endDateTime") java.time.LocalDateTime endDateTime);

    // Methods for Status Changes Activity
    @Query("SELECT e FROM Employee e " +
            "JOIN FETCH e.user u " +
            "LEFT JOIN FETCH e.department d " +
            "LEFT JOIN FETCH e.position p " +
            "WHERE e.hireDate >= :since " +
            "ORDER BY e.hireDate DESC")
    List<Employee> findNewHires(@Param("since") LocalDate since);

    @Query("SELECT e FROM Employee e " +
            "JOIN FETCH e.user u " +
            "LEFT JOIN FETCH e.department d " +
            "LEFT JOIN FETCH e.position p " +
            "WHERE e.status = 'TERMINATED' " +
            "AND e.terminationDate >= :since " +
            "ORDER BY e.terminationDate DESC")
    List<Employee> findRecentTerminations(@Param("since") LocalDate since);

    @Query("SELECT COUNT(e) FROM Employee e WHERE e.updatedAt >= :since")
    long countByUpdatedAtAfter(@Param("since") java.time.LocalDateTime since);

    // ── HR Reports: Aggregation Queries (read-only) ──────────────────────────

    @Query("SELECT d.name, COUNT(e) FROM Employee e JOIN e.department d " +
            "WHERE e.status = 'ACTIVE' GROUP BY d.name ORDER BY COUNT(e) DESC")
    List<Object[]> countActiveByDepartment();

    @Query("SELECT e.status, COUNT(e) FROM Employee e GROUP BY e.status")
    List<Object[]> countByStatusGrouped();

    @Query("SELECT COUNT(e) FROM Employee e WHERE e.status = 'ACTIVE' " +
            "AND YEAR(e.hireDate) = :year AND MONTH(e.hireDate) = :month")
    long countNewHiresInMonth(@Param("year") int year, @Param("month") int month);

    @Query("SELECT COUNT(e) FROM Employee e WHERE e.status = 'TERMINATED' " +
            "AND e.terminationDate IS NOT NULL " +
            "AND YEAR(e.terminationDate) = :year AND MONTH(e.terminationDate) = :month")
    long countTerminationsInMonth(@Param("year") int year, @Param("month") int month);

    // ── Analytics: Retention Rate Calculation ────────────────────────────────

    /**
     * Count employees who were active at the start of the period
     */
    @Query("SELECT COUNT(e) FROM Employee e WHERE e.hireDate < :periodStart")
    long countEmployeesAtPeriodStart(@Param("periodStart") LocalDate periodStart);

    /**
     * Count employees who left during the period
     */
    @Query("SELECT COUNT(e) FROM Employee e WHERE e.status = 'TERMINATED' " +
            "AND e.terminationDate >= :periodStart AND e.terminationDate < :periodEnd")
    long countTerminationsBetween(@Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd);

    /**
     * Count workforce changes (new hires) in a period
     */
    @Query("SELECT COUNT(e) FROM Employee e WHERE e.hireDate >= :periodStart AND e.hireDate < :periodEnd")
    long countNewHiresBetween(@Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd);

    /**
     * Find employees without department assignment (unassigned employees)
     */
    List<Employee> findByDepartmentIdIsNull();

}
