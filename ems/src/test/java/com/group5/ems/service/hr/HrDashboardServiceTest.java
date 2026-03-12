package com.group5.ems.service.hr;

import com.group5.ems.dto.response.HrDashboardMetricsDTO;
import com.group5.ems.repository.ApplicationRepository;
import com.group5.ems.repository.AttendanceRepository;
import com.group5.ems.repository.EmployeeRepository;
import com.group5.ems.repository.JobPostRepository;
import com.group5.ems.repository.RequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HrDashboardServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private JobPostRepository jobPostRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @InjectMocks
    private HrDashboardService hrDashboardService;

    @Test
    void testGetDashboardMetrics() {
        // Arrange
        when(employeeRepository.countByStatus("ACTIVE")).thenReturn(42L);
        when(requestRepository.countByStatusAndRequestTypeCategory("PENDING", "ATTENDANCE")).thenReturn(5);
        when(jobPostRepository.countByStatus("OPEN")).thenReturn(2);
        when(requestRepository.countByStatusAndRequestTypeCategory("PENDING", "HR_STATUS")).thenReturn(10);
        when(employeeRepository.newThisMonth()).thenReturn(3L);
        when(applicationRepository.count()).thenReturn(100L);

        when(attendanceRepository.countByWorkDateAndStatus(any(LocalDate.class), eq("PRESENT"))).thenReturn(30);
        when(attendanceRepository.countByWorkDateAndStatus(any(LocalDate.class), eq("ON_LEAVE"))).thenReturn(5);
        when(attendanceRepository.countByWorkDate(any(LocalDate.class))).thenReturn(40);

        when(applicationRepository.countByStatus("APPLIED")).thenReturn(20);
        when(applicationRepository.countByStatus("REVIEWING")).thenReturn(10);
        when(applicationRepository.countByStatus("INTERVIEWING")).thenReturn(5);
        when(applicationRepository.countByStatus("OFFER_SENT")).thenReturn(2);

        // Act
        HrDashboardMetricsDTO result = hrDashboardService.getDashboardMetrics();

        // Assert
        assertEquals(42, result.activeEmployees());
        assertEquals(5, result.pendingLeaveRequests());
        assertEquals(2, result.openJobPosts());
        assertEquals(10, result.pendingWorkflowRequests());
        assertEquals(3, result.newHiresThisMonth());
        assertEquals(100, result.totalApplicants());
    }
}