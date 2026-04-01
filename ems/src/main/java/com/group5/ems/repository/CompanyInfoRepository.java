package com.group5.ems.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.group5.ems.entity.CompanyInfo;

public interface CompanyInfoRepository extends JpaRepository<CompanyInfo, Long> {

    Optional<CompanyInfo> findByInfoKey(String infoKey);

    List<CompanyInfo> findByIsPublicTrue();

    boolean existsByInfoKey(String infoKey);
}
