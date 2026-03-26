package com.group5.ems.service.hr;

import com.group5.ems.dto.hr.PayrollRunSummaryDTO;
import com.group5.ems.dto.hr.PayslipReviewDTO;
import com.group5.ems.entity.TimesheetPeriod;
import com.group5.ems.enums.AuditAction;
import com.group5.ems.enums.AuditEntityType;
import com.group5.ems.repository.PayslipRepository;
import com.group5.ems.repository.TimesheetPeriodRepository;
import com.group5.ems.service.hr.impl.PayrollReviewServiceImpl;
import com.group5.ems.service.common.LogService;
import com.group5.ems.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayrollReviewServiceTest {

    @Mock
    private PayslipRepository payslipRepository;

    @Mock
    private TimesheetPeriodRepository periodRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LogService logService;

    @InjectMocks
    private PayrollReviewServiceImpl payrollReviewService;

    private TimesheetPeriod period;

    @BeforeEach
    void setUp() {
        period = new TimesheetPeriod();
        period.setId(1L);
        period.setPeriodName("March 2026");
    }

    @Test
    void getPaginatedReview_ShouldReturnPage() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<PayslipReviewDTO> page = new PageImpl<>(Collections.emptyList());

        when(payslipRepository.findReviewDTOByPeriodId(1L, pageable)).thenReturn(page);

        Page<PayslipReviewDTO> result = payrollReviewService.getPaginatedReview(1L, pageable);

        assertNotNull(result);
        verify(payslipRepository).findReviewDTOByPeriodId(1L, pageable);
    }

    @Test
    void getRunSummary_ShouldCalculateCorrectly() {
        when(periodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(payslipRepository.countByPeriodId(1L)).thenReturn(10);
        when(payslipRepository.sumTotalGrossByPeriodId(1L)).thenReturn(new BigDecimal("1000.00"));
        when(payslipRepository.sumTotalNetByPeriodId(1L)).thenReturn(new BigDecimal("800.00"));
        when(payslipRepository.countPendingByPeriodId(1L)).thenReturn(5L);

        PayrollRunSummaryDTO summary = payrollReviewService.getRunSummary(1L);

        assertEquals("March 2026", summary.periodName());
        assertEquals(10, summary.totalPayslips());
        assertEquals(new BigDecimal("1000.00"), summary.companyTotalGross());
        assertEquals(new BigDecimal("800.00"), summary.companyTotalNet());
        assertFalse(summary.isFullyApproved());
    }

    @Test
    void approvePayrollRun_ShouldThrowExceptionIfNoPending() {
        when(payslipRepository.countPendingByPeriodId(1L)).thenReturn(0L);

        assertThrows(IllegalStateException.class, () -> payrollReviewService.approvePayrollRun(1L));
    }

    @Test
    void approvePayrollRun_ShouldCallRepositoryAndLog() {
        // Mock Security Context
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("admin");
        
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(new com.group5.ems.entity.User()));
        when(payslipRepository.countPendingByPeriodId(1L)).thenReturn(5L);

        payrollReviewService.approvePayrollRun(1L);

        verify(payslipRepository).approveAllPendingInPeriod(eq(1L), any());
        verify(logService).log(eq(AuditAction.UPDATE), eq(AuditEntityType.PAYROLL), eq(1L));
    }
}
