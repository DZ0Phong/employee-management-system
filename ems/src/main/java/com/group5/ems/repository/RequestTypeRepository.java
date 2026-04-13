package com.group5.ems.repository;

import com.group5.ems.entity.RequestType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RequestTypeRepository extends JpaRepository<RequestType, Long> {

    Optional<RequestType> findByCode(String code);

    List<RequestType> findByCategoryIn(List<String> categories);

    List<RequestType> findByCategoryNotInOrderByCategoryAscNameAsc(List<String> excludedCategories);

    List<RequestType> findByCategoryAndCodeStartingWithOrderByNameAsc(String category, String codePrefix);
}

