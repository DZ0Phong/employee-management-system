package com.group5.ems.repository;

import com.group5.ems.entity.PerformanceReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PerformanceReviewRepository extends JpaRepository<PerformanceReview, Long> {

    // ── Existing methods (used by employee role — DO NOT REMOVE) ──
    List<PerformanceReview> findByEmployeeId(Long employeeId);


    List<PerformanceReview> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);

    // ── New methods for HR role ──────────────────────────────────

    /** Search + status + advanced filters with pagination */
    @Query(value = """
           SELECT pr FROM PerformanceReview pr 
           JOIN FETCH pr.employee e 
           JOIN FETCH e.user u 
           LEFT JOIN FETCH e.department d 
           LEFT JOIN FETCH pr.reviewer r 
           LEFT JOIN FETCH r.user ru 
           WHERE (:search IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%'))) 
           AND (:status IS NULL OR pr.status = :status) 
           AND (:departmentId IS NULL OR e.department.id = :departmentId) 
           AND (:reviewerId IS NULL OR pr.reviewerId = :reviewerId) 
           AND (:reviewPeriod IS NULL OR pr.reviewPeriod = :reviewPeriod) 
           AND (:minScore IS NULL OR pr.performanceScore >= :minScore) 
           AND (:maxScore IS NULL OR pr.performanceScore <= :maxScore) 
           AND (:minPotential IS NULL OR pr.potentialScore >= :minPotential) 
           AND (:maxPotential IS NULL OR pr.potentialScore <= :maxPotential)
           """,
           countQuery = """
           SELECT count(pr) FROM PerformanceReview pr 
           JOIN pr.employee e JOIN e.user u 
           WHERE (:search IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%'))) 
           AND (:status IS NULL OR pr.status = :status) 
           AND (:departmentId IS NULL OR e.department.id = :departmentId) 
           AND (:reviewerId IS NULL OR pr.reviewerId = :reviewerId) 
           AND (:reviewPeriod IS NULL OR pr.reviewPeriod = :reviewPeriod) 
           AND (:minScore IS NULL OR pr.performanceScore >= :minScore) 
           AND (:maxScore IS NULL OR pr.performanceScore <= :maxScore) 
           AND (:minPotential IS NULL OR pr.potentialScore >= :minPotential) 
           AND (:maxPotential IS NULL OR pr.potentialScore <= :maxPotential)
           """)
    Page<PerformanceReview> searchAdvanced(
            @Param("search") String search,
            @Param("status") String status,
            @Param("departmentId") Long departmentId,
            @Param("reviewerId") Long reviewerId,
            @Param("reviewPeriod") String reviewPeriod,
            @Param("minScore") java.math.BigDecimal minScore,
            @Param("maxScore") java.math.BigDecimal maxScore,
            @Param("minPotential") java.math.BigDecimal minPotential,
            @Param("maxPotential") java.math.BigDecimal maxPotential,
            Pageable pageable);

    /** Count by status for stats cards */
    long countByStatus(String status);

    /** Get all distinct review periods for filtering */
    @Query("""
        SELECT DISTINCT pr.reviewPeriod FROM PerformanceReview pr ORDER BY pr.reviewPeriod DESC
    """)
    List<String> findDistinctReviewPeriods();

    /** Get all distinct statuses for filtering */
    @Query("""
        SELECT DISTINCT pr.status FROM PerformanceReview pr ORDER BY pr.status
    """)
    List<String> findDistinctStatuses();

    // ── New methods for Dept Manager role ────────────────────────
    @EntityGraph(attributePaths = {"employee", "employee.user", "employee.position", "reviewer", "reviewer.user"})
    List<PerformanceReview> findByEmployee_DepartmentIdOrderByUpdatedAtDesc(Long departmentId);

    @EntityGraph(attributePaths = {"employee", "employee.user", "employee.position", "reviewer", "reviewer.user"})
    Optional<PerformanceReview> findByIdAndEmployee_DepartmentId(Long id, Long departmentId);

    Optional<PerformanceReview> findByEmployeeIdAndReviewPeriod(Long employeeId, String reviewPeriod);

    @EntityGraph(attributePaths = {"employee", "employee.user", "employee.position", "reviewer", "reviewer.user"})
    List<PerformanceReview> findByEmployeeIdInOrderByUpdatedAtDesc(List<Long> employeeIds);

    // ── HR Reports: Aggregation Queries (read-only) ──────────────────────────

    @Query("""
        SELECT AVG(pr.performanceScore), AVG(pr.potentialScore) FROM PerformanceReview pr 
        WHERE pr.status IN ('COMPLETED', 'PUBLISHED')
    """)
    List<Object[]> getAvgScores();

    @Query("""
        SELECT 
            CASE 
                WHEN pr.performanceScore >= 4.5 THEN 'A'
                WHEN pr.performanceScore >= 3.5 THEN 'B'
                WHEN pr.performanceScore >= 2.5 THEN 'C'
                WHEN pr.performanceScore >= 1.5 THEN 'D'
                ELSE 'F'
            END, 
            COUNT(pr) 
        FROM PerformanceReview pr 
        WHERE pr.status IN ('COMPLETED', 'PUBLISHED') 
        GROUP BY 
            CASE 
                WHEN pr.performanceScore >= 4.5 THEN 'A'
                WHEN pr.performanceScore >= 3.5 THEN 'B'
                WHEN pr.performanceScore >= 2.5 THEN 'C'
                WHEN pr.performanceScore >= 1.5 THEN 'D'
                ELSE 'F'
            END
    """)
    List<Object[]> countByPerformanceGradeGrouped();

    @Query("""
           SELECT
             CASE
               WHEN pr.performanceScore >= 4.0 AND pr.potentialScore >= 4.0 THEN 'Star'
               WHEN pr.performanceScore >= 4.0 AND pr.potentialScore >= 2.5 THEN 'High Performer'
               WHEN pr.performanceScore >= 4.0 THEN 'Workhorse'
               WHEN pr.performanceScore >= 2.5 AND pr.potentialScore >= 4.0 THEN 'High Potential'
               WHEN pr.performanceScore >= 2.5 AND pr.potentialScore >= 2.5 THEN 'Core Employee'
               WHEN pr.performanceScore >= 2.5 THEN 'Effective'
               WHEN pr.potentialScore >= 4.0 THEN 'Problem Child'
               WHEN pr.potentialScore >= 2.5 THEN 'Inconsistent'
               ELSE 'Underperformer'
             END,
             COUNT(pr)
           FROM PerformanceReview pr
           WHERE pr.status = 'COMPLETED'
           GROUP BY
             CASE
               WHEN pr.performanceScore >= 4.0 AND pr.potentialScore >= 4.0 THEN 'Star'
               WHEN pr.performanceScore >= 4.0 AND pr.potentialScore >= 2.5 THEN 'High Performer'
               WHEN pr.performanceScore >= 4.0 THEN 'Workhorse'
               WHEN pr.performanceScore >= 2.5 AND pr.potentialScore >= 4.0 THEN 'High Potential'
               WHEN pr.performanceScore >= 2.5 AND pr.potentialScore >= 2.5 THEN 'Core Employee'
               WHEN pr.performanceScore >= 2.5 THEN 'Effective'
               WHEN pr.potentialScore >= 4.0 THEN 'Problem Child'
               WHEN pr.potentialScore >= 2.5 THEN 'Inconsistent'
               ELSE 'Underperformer'
             END
           """)
    List<Object[]> countByTalentMatrixGrouped();

    @Query("SELECT u.fullName, d.name, pr.performanceScore FROM PerformanceReview pr " +
           "JOIN pr.employee e JOIN e.user u LEFT JOIN e.department d " +
           "WHERE pr.status = 'COMPLETED' " +
           "ORDER BY pr.performanceScore DESC")
    List<Object[]> findTopPerformers(Pageable pageable);

    @Query("""
        SELECT new com.group5.ems.dto.response.hr.TopPerformerDTO(e.id, AVG(pr.performanceScore))
        FROM PerformanceReview pr
        JOIN pr.employee e
        WHERE pr.status IN ('COMPLETED', 'PUBLISHED') AND function('YEAR', pr.createdAt) = :year
        GROUP BY e.id
        ORDER BY AVG(pr.performanceScore) DESC
    """)
    List<com.group5.ems.dto.response.hr.TopPerformerDTO> findTopPerformersByYear(@Param("year") int year);
}
