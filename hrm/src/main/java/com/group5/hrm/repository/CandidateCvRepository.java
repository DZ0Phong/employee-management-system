package com.group5.hrm.repository;

import com.group5.hrm.entity.CandidateCv;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CandidateCvRepository extends JpaRepository<CandidateCv, Long> {

    List<CandidateCv> findByCandidateId(Long candidateId);
}
