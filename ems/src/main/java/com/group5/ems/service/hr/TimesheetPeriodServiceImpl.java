package com.group5.ems.service.hr;

import com.group5.ems.dto.request.PeriodCreateReq;
import com.group5.ems.entity.TimesheetPeriod;
import com.group5.ems.enums.AuditAction;
import com.group5.ems.enums.AuditEntityType;
import com.group5.ems.exception.PeriodAlreadyLockedException;
import com.group5.ems.exception.PeriodOverlapException;
import com.group5.ems.repository.TimesheetPeriodRepository;
import com.group5.ems.repository.UserRepository;
import com.group5.ems.service.common.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class TimesheetPeriodServiceImpl implements TimesheetPeriodService {

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final TimesheetPeriodRepository periodRepository;
    private final UserRepository userRepository;
    private final LogService logService;

    @Override
    @Transactional(readOnly = true)
    public Page<TimesheetPeriod> getPeriods(Pageable pageable) {
        // Force sort by startDate descending regardless of incoming sort
        Pageable sorted = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "startDate")
        );
        return periodRepository.findAll(sorted);
    }

    @Override
    @Transactional
    public void createPeriod(PeriodCreateReq req) {
        // Check for overlapping periods
        if (periodRepository.isDateRangeOverlapping(req.startDate(), req.endDate())) {
            throw new PeriodOverlapException();
        }

        TimesheetPeriod period = new TimesheetPeriod();
        period.setPeriodName(req.periodName());
        period.setStartDate(req.startDate());
        period.setEndDate(req.endDate());
        period.setIsLocked(false);

        TimesheetPeriod saved = periodRepository.save(period);

        // Rule #15: Log create action
        logService.log(AuditAction.CREATE, AuditEntityType.TIMESHEET_PERIOD, saved.getId());
    }

    @Override
    @Transactional
    public void lockPeriod(Long periodId) {
        TimesheetPeriod period = periodRepository.findById(periodId)
                .orElseThrow(() -> new IllegalArgumentException("Timesheet period not found with ID: " + periodId));

        if (Boolean.TRUE.equals(period.getIsLocked())) {
            throw new PeriodAlreadyLockedException();
        }

        period.setIsLocked(true);
        period.setLockedAt(LocalDateTime.now(VIETNAM_ZONE));
        period.setLockedBy(resolveCurrentUserId());

        periodRepository.save(period);

        // Rule #15: Log lock action
        logService.log(AuditAction.UPDATE, AuditEntityType.TIMESHEET_PERIOD, periodId);
    }

    /**
     * Resolves the current authenticated user's ID from SecurityContextHolder.
     */
    private Long resolveCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        return userRepository.findByUsername(auth.getName())
                .map(user -> user.getId())
                .orElse(null);
    }
}
