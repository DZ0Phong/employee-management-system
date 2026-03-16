package com.group5.ems.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.group5.ems.entity.Application;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    List<Application> findByJobPostId(Long jobPostId);

    List<Application> findByCandidateId(Long candidateId);

    Optional<Application> findByJobPostIdAndCandidateId(Long jobPostId, Long candidateId);

    List<Application> findByJobPostIdAndStatus(Long jobPostId, String status);

<<<<<<< Updated upstream
    Application findByTrackingToken(String trackingToken);
}
=======
    int countByStatus(String status);


    Optional<Application> findByTrackingToken(String token);

    void deleteByTrackingToken(String token);
}
>>>>>>> Stashed changes
