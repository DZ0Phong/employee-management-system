package com.group5.ems.service.hr;

import com.group5.ems.dto.response.HrPayrollDTO;
import com.group5.ems.entity.Department;
import com.group5.ems.entity.Employee;
import com.group5.ems.entity.Payslip;
import com.group5.ems.entity.Position;
import com.group5.ems.entity.TimesheetPeriod;
import com.group5.ems.entity.User;
import com.group5.ems.repository.PayslipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HrPayrollServiceTest {

    @Mock
    private PayslipRepository payslipRepository;

    @InjectMocks
    private HrPayrollService hrPayrollService;

    private Payslip payslip1;

    @BeforeEach
    void setUp() {
        User user1 = new User();
        user1.setFullName("Bob Builder");

        Department dept1 = new Department();
        dept1.setName("Construction");

        Position pos1 = new Position();
        pos1.setName("Foreman");

        Employee employee1 = new Employee();
        employee1.setUser(user1);
        employee1.setDepartment(dept1);
        employee1.setPosition(pos1);

        TimesheetPeriod period1 = new TimesheetPeriod();
        period1.setStartDate(LocalDate.of(2023, 10, 1));
        period1.setEndDate(LocalDate.of(2023, 10, 31));

        payslip1 = new Payslip();
        payslip1.setId(100L);
        payslip1.setEmployee(employee1);
        payslip1.setPeriod(period1);
        payslip1.setActualBaseSalary(new BigDecimal("5000.00"));
        payslip1.setTotalBonus(new BigDecimal("500.00"));
        payslip1.setTotalDeduction(new BigDecimal("200.00"));
        payslip1.setNetSalary(new BigDecimal("5300.00"));
        payslip1.setStatus("PAID");
    }

    @Test
    void testGetAllPayslips() {
        when(payslipRepository.findAll()).thenReturn(Arrays.asList(payslip1));

        List<HrPayrollDTO> result = hrPayrollService.getAllPayslips();

        assertEquals(1, result.size());
        HrPayrollDTO dto = result.get(0);
        assertEquals("Bob Builder", dto.employeeName());
        assertEquals("Foreman", dto.position());
        assertEquals("Construction", dto.department());
        assertEquals(new BigDecimal("5000.00"), dto.basicSalary());
        assertEquals(new BigDecimal("500.00"), dto.totalAllowances());
        assertEquals(new BigDecimal("200.00"), dto.totalDeductions());
        assertEquals(new BigDecimal("5300.00"), dto.netSalary());
        assertEquals("PAID", dto.status());
        assertEquals(LocalDate.of(2023, 10, 31), dto.paymentDate());
    }
}
