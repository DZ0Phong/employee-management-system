package com.group5.ems.repository;

import com.group5.ems.entity.PayComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PayComponentRepository extends JpaRepository<PayComponent, Long> {
    Optional<PayComponent> findByCode(String code);
}
