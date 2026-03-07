package com.group5.ems.repository;

import com.group5.ems.entity.CandidateCv;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CandidateCvRepository extends JpaRepository<CandidateCv, Long> {

    List<CandidateCv> findByCandidateId(Long candidateId);
}
