package com.group5.ems.service.hr;

import com.group5.ems.dto.response.HrDashboardMetricsDTO;
import com.group5.ems.dto.response.HrDashboardMetricsDTO;
import com.group5.ems.repository.EmployeeRepository;
import com.group5.ems.repository.JobPostRepository;
import com.group5.ems.repository.RequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HrDashboardServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private JobPostRepository jobPostRepository;

    @InjectMocks
    private HrDashboardService hrDashboardService;

    @Test
    void testGetDashboardMetrics() {
        // Arrange
        when(employeeRepository.countByStatus("ACTIVE")).thenReturn(42L);
        when(requestRepository.countByStatusAndRequestTypeCategory("PENDING", "ATTENDANCE")).thenReturn(5);
        when(jobPostRepository.countByStatus("OPEN")).thenReturn(2);
        when(requestRepository.countByStatusAndRequestTypeCategory("PENDING", "HR_STATUS")).thenReturn(10);

        // Act
        HrDashboardMetricsDTO result = hrDashboardService.getDashboardMetrics();

        // Assert
        assertEquals(42, result.activeEmployees());
        assertEquals(5, result.pendingLeaveRequests());
        assertEquals(2, result.openJobPosts());
        assertEquals(10, result.pendingWorkflowRequests());
    }
}
