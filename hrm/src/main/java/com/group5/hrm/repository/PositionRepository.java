package com.group5.hrm.repository;

import com.group5.hrm.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PositionRepository extends JpaRepository<Position, Long> {

    Optional<Position> findByCode(String code);

    List<Position> findByDepartmentId(Long departmentId);
}

