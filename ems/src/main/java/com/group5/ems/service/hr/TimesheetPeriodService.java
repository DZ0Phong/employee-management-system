package com.group5.ems.service.hr;

import com.group5.ems.dto.request.PeriodCreateReq;
import com.group5.ems.entity.TimesheetPeriod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for Timesheet Period management (HR module).
 */
public interface TimesheetPeriodService {

    /**
     * Retrieves a paginated list of timesheet periods, sorted by startDate descending.
     */
    Page<TimesheetPeriod> getPeriods(Pageable pageable);

    /**
     * Creates a new timesheet period after checking for date range overlap.
     *
     * @throws com.group5.ems.exception.PeriodOverlapException if the date range overlaps with an existing period
     */
    void createPeriod(PeriodCreateReq req);

    /**
     * Locks a timesheet period, recording the timestamp and the user who performed the lock.
     *
     * @throws com.group5.ems.exception.PeriodAlreadyLockedException if the period is already locked
     */
    void lockPeriod(Long periodId);
}
