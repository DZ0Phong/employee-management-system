package com.group5.ems.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.group5.ems.entity.CandidateCv;

public interface CandidateCvRepository extends JpaRepository<CandidateCv, Long> {

    /**
     * Full entity list for a candidate (includes BLOB — use sparingly).
     */
    List<CandidateCv> findByCandidateIdOrderByUploadedAtDesc(Long candidateId);

    /**
     * Metadata-only projection — avoids loading BLOB into memory for list/modal views.
     * Returns Object[] rows: [id, fileName, fileType, uploadedAt]
     * uploadedAt may be LocalDateTime or java.sql.Timestamp depending on the JDBC driver;
     * the service layer handles both types safely.
     */
    @Query("SELECT c.id, c.fileName, c.fileType, c.uploadedAt " +
           "FROM CandidateCv c " +
           "WHERE c.candidateId = :candidateId " +
           "ORDER BY c.uploadedAt DESC")
    List<Object[]> findMetadataByCandidateId(@Param("candidateId") Long candidateId);

    Optional<CandidateCv> findByIdAndCandidateId(Long id, Long candidateId);

    long countByCandidateId(Long candidateId);

    void deleteByCandidateId(Long candidateId);

    List<CandidateCv> findByCandidateId(Long candidateId);
}