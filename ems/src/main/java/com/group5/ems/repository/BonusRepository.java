package com.group5.ems.repository;

import com.group5.ems.entity.Bonus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BonusRepository extends JpaRepository<Bonus, Long> {

    List<Bonus> findByEmployeeId(Long employeeId);

    List<Bonus> findByEmployeeIdAndStatus(Long employeeId, String status);
}

