package com.group5.ems.repository;

import com.group5.ems.entity.BenefitType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BenefitTypeRepository extends JpaRepository<BenefitType, Long> {

    Optional<BenefitType> findByCode(String code);

    List<BenefitType> findByIsActiveTrue();
}

