package com.group5.ems.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.group5.ems.entity.JobPost;
import java.util.Optional;

public interface JobPostRepository extends JpaRepository<JobPost, Long> {

    @EntityGraph(attributePaths = {"department", "position"})
    Optional<JobPost> findById(Long id);

    @EntityGraph(attributePaths = {"department", "position"})
    List<JobPost> findByStatus(String status);

    @EntityGraph(attributePaths = {"department", "position"})
    List<JobPost> findByDepartmentId(Long departmentId);

    @EntityGraph(attributePaths = {"department", "position"})
    List<JobPost> findByCreatedBy(Long createdBy);

    int countByStatus(String status);


    @Query("SELECT COUNT(DISTINCT j.departmentId) FROM JobPost j")
    long countDistinctDepartment();

    @Query("""
            SELECT COUNT(j)
            FROM JobPost j
            WHERE j.departmentId = :deptId
            """)
    long countByDepartment(Long deptId);
}
