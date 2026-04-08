package com.group5.ems.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.group5.ems.entity.JobPost;

public interface JobPostRepository extends JpaRepository<JobPost, Long> {

    @EntityGraph(attributePaths = { "department", "position" })
    Optional<JobPost> findById(Long id);

    @EntityGraph(attributePaths = { "department", "position" })
    List<JobPost> findByStatus(String status);

    @EntityGraph(attributePaths = { "department", "position" })
    List<JobPost> findByDepartmentId(Long departmentId);

    @EntityGraph(attributePaths = { "department", "position" })
    List<JobPost> findByCreatedBy(Long createdBy);

    int countByStatus(String status);

    @Query("SELECT COUNT(DISTINCT j.departmentId) FROM JobPost j")
    long countDistinctDepartment();

    @Query("""
            SELECT COUNT(j)
            FROM JobPost j
            WHERE j.departmentId = :deptId AND j.status = 'OPEN'
            """)
    long countOpenByDepartment(Long deptId);

    long countByDepartmentId(Long departmentId);

    @Query("SELECT j.title FROM JobPost j WHERE j.id = :id")
    Optional<String> findTitleById(Long id);

    // Method for Dashboard KPI calculations
    @Query("SELECT j FROM JobPost j WHERE j.status = 'OPEN' AND DATE(j.createdAt) <= :date")
    List<JobPost> findOpenJobsAtDate(@org.springframework.data.repository.query.Param("date") java.time.LocalDate date);
}
