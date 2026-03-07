package com.group5.ems.repository;

import com.group5.ems.entity.Offer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OfferRepository extends JpaRepository<Offer, Long> {

    List<Offer> findByApplicationId(Long applicationId);
}
