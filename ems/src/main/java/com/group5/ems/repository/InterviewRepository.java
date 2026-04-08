package com.group5.ems.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.group5.ems.entity.Interview;

public interface InterviewRepository extends JpaRepository<Interview, Long> {

    List<Interview> findByApplicationId(Long applicationId);

    List<Interview> findByInterviewerId(Long interviewerId);

    Optional<Interview> findByApplicationIdAndInterviewerId(Long applicationId, Long interviewerId);

    /**
     * Lấy tất cả lịch phỏng vấn của 1 interviewer (My Interviews).
   **/
    @Query("""
                SELECT iv FROM Interview iv
                LEFT JOIN FETCH iv.application app
                LEFT JOIN FETCH app.candidate
                LEFT JOIN FETCH app.jobPost jp
                LEFT JOIN FETCH jp.department
                WHERE iv.interviewerId = :interviewerId
                ORDER BY iv.scheduledAt DESC
            """)
    List<Interview> findByInterviewerIdOrderByScheduledAtDesc(@Param("interviewerId") Long interviewerId);

    /**
     * Lấy tất cả interviews của 1 application (candidate detail modal).
     */
    @Query("""
                SELECT iv FROM Interview iv
                LEFT JOIN FETCH iv.application app
                LEFT JOIN FETCH app.candidate
                LEFT JOIN FETCH app.jobPost jp
                LEFT JOIN FETCH jp.department
                WHERE iv.applicationId = :applicationId
                ORDER BY iv.scheduledAt DESC
            """)
    List<Interview> findByApplicationIdOrderByScheduledAtDesc(@Param("applicationId") Long applicationId);
}
