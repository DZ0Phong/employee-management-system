package com.group5.hrm.repository;

import com.group5.hrm.entity.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CandidateRepository extends JpaRepository<Candidate, Long> {

    Optional<Candidate> findByEmail(String email);

    List<Candidate> findByEmailOrPhone(String email, String phone);
}

