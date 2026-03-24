package com.group5.ems.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.group5.ems.entity.InterviewAssignment;

public interface InterviewAssignmentRepository extends JpaRepository<InterviewAssignment, Long> {

    @EntityGraph(attributePaths = { "interviewer" })
    List<InterviewAssignment> findByApplicationId(Long applicationId);

    boolean existsByApplicationIdAndInterviewerId(Long applicationId, Long interviewerId);

    @Modifying
    @Query("DELETE FROM InterviewAssignment ia WHERE ia.applicationId = :applicationId")
    void deleteByApplicationId(Long applicationId);

    List<InterviewAssignment> findByInterviewerId(Long interviewerId);
}