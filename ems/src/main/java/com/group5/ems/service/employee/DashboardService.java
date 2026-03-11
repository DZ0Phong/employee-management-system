package com.group5.ems.service.employee;

import com.group5.ems.dto.response.ActivityDTO;
import com.group5.ems.dto.response.EmployeeDashboardDTO;
import com.group5.ems.dto.response.EmployeeInfoDTO;

import java.util.List;

public interface DashboardService {
    EmployeeInfoDTO getEmployeeInfo(Long employeeId, Long userId);
    EmployeeDashboardDTO getDashboardData(Long employeeId);
    List<ActivityDTO> getRecentActivities(Long employeeId);
}