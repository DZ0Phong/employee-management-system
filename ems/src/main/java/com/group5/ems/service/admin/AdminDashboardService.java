package com.group5.ems.service.admin;

import com.group5.ems.dto.response.UserDTO;

import java.util.List;

public interface AdminDashboardService {
    int getAllActiveEmployees();
    int getAllInactiveEmployees();
    int getAllSuspendedEmployees();
    int getAllEmployeesCount();
    long getNewThisMonth();
    Double getActiveRate();
    Double getInactiveRate();
    Double getSuspendedRate();
    long getNewThisYear();
    int getAllDepartmentsCount();
    List<String> getAllDepartmentsName();
    List<Object []> getAllDepartmentsPercentage();
    List<String> getHeadcountMonths(int months);
    List<Integer> getHeadcountTotal(int months);
    List<Integer> getHeadcountActive(int months);
    List<Integer> getHeadcountSuspended(int months);
    List<UserDTO> getTop5RecentUser();
}
