package com.group5.ems.repository;

import com.group5.ems.entity.CompanyInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyInfoRepository extends JpaRepository<CompanyInfo, Long> {

    Optional<CompanyInfo> findByInfoKey(String infoKey);

    List<CompanyInfo> findByIsPublicTrue();
}

