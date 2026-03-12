package com.group5.ems.repository;

import com.group5.ems.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    List<Application> findByJobPostId(Long jobPostId);

    List<Application> findByCandidateId(Long candidateId);

    Optional<Application> findByJobPostIdAndCandidateId(Long jobPostId, Long candidateId);

    List<Application> findByJobPostIdAndStatus(Long jobPostId, String status);

    int countByStatus(String status);

    Application findByTrackingToken(String trackingToken);
}