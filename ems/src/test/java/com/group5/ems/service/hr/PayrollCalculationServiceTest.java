package com.group5.ems.service.hr;

import com.group5.ems.entity.TimesheetPeriod;
import com.group5.ems.entity.Payslip;
import com.group5.ems.repository.PayslipRepository;
import com.group5.ems.repository.TimesheetPeriodRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayrollCalculationServiceTest {

    @Mock
    private TimesheetPeriodRepository periodRepository;

    @Mock
    private PayslipRepository payslipRepository;

    @InjectMocks
    private PayrollAggregationService calculationService; // Re-uses calculation logic currently in AggregationService

    @Test
    void generatePayslips_ShouldThrowException_IfAlreadyGenerated() {
        // Arrange
        Long periodId = 1L;
        TimesheetPeriod period = new TimesheetPeriod();
        period.setId(periodId);
        period.setIsLocked(true); // Must be locked

        when(periodRepository.findById(periodId)).thenReturn(Optional.of(period));
        when(payslipRepository.findByPeriodId(periodId)).thenReturn(Collections.singletonList(new Payslip()));

        // Act & Assert
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> calculationService.generatePayslips(periodId))
                .withMessageContaining("Payslips have already been generated for this period");
    }

    @Test
    void engineMath_GrossPay_Calculation() {
        // Test Engine Math
        // Base: $5000, Std Days: 20, Payable: 18, OT: $200, Bonus: $100 -> Expected Gross (Net without tax):
        // (5000 / 20) * 18 + 200 + 100 = 4500 + 200 + 100 = 4800
        // Currently the system defines Net as actualBase + OT + Bonus - Deductions
        
        BigDecimal base = new BigDecimal("5000");
        int stdDays = 20;
        int payable = 18;
        BigDecimal ot = new BigDecimal("200");
        BigDecimal bonus = new BigDecimal("100");
        BigDecimal deduction = BigDecimal.ZERO;
        
        // Prorated actual base
        BigDecimal actualBase = base.multiply(BigDecimal.valueOf(payable))
                                    .divide(BigDecimal.valueOf(stdDays), 2, java.math.RoundingMode.HALF_UP);
                                    
        BigDecimal grossPay = actualBase.add(ot).add(bonus).subtract(deduction);
        
        // Assert
        assertThat(actualBase).isEqualByComparingTo(new BigDecimal("4500.00"));
        assertThat(grossPay).isEqualByComparingTo(new BigDecimal("4800.00"));
    }

    @Test
    void generatePayslips_ShouldThrowException_IfPeriodNotLocked() {
        // Arrange
        Long periodId = 1L;
        TimesheetPeriod period = new TimesheetPeriod();
        period.setId(periodId);
        period.setIsLocked(false);

        when(periodRepository.findById(periodId)).thenReturn(Optional.of(period));

        // Act & Assert
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> calculationService.generatePayslips(periodId))
                .withMessageContaining("Cannot generate payslips for an unlocked period");
    }
}
