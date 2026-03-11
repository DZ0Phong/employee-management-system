package com.group5.ems.service.admin;

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
}
