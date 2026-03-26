package com.group5.ems.repository;

import com.group5.ems.entity.TimesheetPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TimesheetPeriodRepository extends JpaRepository<TimesheetPeriod, Long> {

    List<TimesheetPeriod> findByIsLocked(Boolean isLocked);

    Optional<TimesheetPeriod> findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqual(LocalDate date, LocalDate dateEnd);

    @Query("SELECT t FROM TimesheetPeriod t WHERE t.startDate < :date AND t.isLocked = true ORDER BY t.startDate DESC LIMIT 1")
    Optional<TimesheetPeriod> findPreviousLockedPeriod(@Param("date") LocalDate date);

    /**
     * Check if a new date range overlaps with any existing period.
     * Uses the standard interval overlap condition: A.start <= B.end AND A.end >= B.start
     */
    @Query("SELECT COUNT(t) > 0 FROM TimesheetPeriod t WHERE t.startDate <= :newEnd AND t.endDate >= :newStart")
    boolean isDateRangeOverlapping(@Param("newStart") LocalDate newStart, @Param("newEnd") LocalDate newEnd);
}
