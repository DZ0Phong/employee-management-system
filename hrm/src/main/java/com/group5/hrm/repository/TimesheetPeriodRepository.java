package com.group5.hrm.repository;

import com.group5.hrm.entity.TimesheetPeriod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TimesheetPeriodRepository extends JpaRepository<TimesheetPeriod, Long> {

    List<TimesheetPeriod> findByIsLocked(Boolean isLocked);

    Optional<TimesheetPeriod> findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqual(LocalDate date, LocalDate dateEnd);
}
