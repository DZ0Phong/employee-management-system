package com.group5.ems.repository;

import com.group5.ems.entity.ApplicationStage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationStageRepository extends JpaRepository<ApplicationStage, Long> {

    List<ApplicationStage> findByApplicationIdOrderByChangedAtDesc(Long applicationId);
}
