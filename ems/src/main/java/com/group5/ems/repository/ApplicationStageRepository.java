package com.group5.ems.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.group5.ems.entity.ApplicationStage;

public interface ApplicationStageRepository extends JpaRepository<ApplicationStage, Long> {

    List<ApplicationStage> findByApplicationIdOrderByChangedAtDesc(Long applicationId);

    List<ApplicationStage> findByApplicationIdOrderByChangedAtAsc(Long applicationId);

    long countByApplicationId(Long applicationId);
}