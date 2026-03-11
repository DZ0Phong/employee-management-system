package com.group5.ems.service.admin;

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
}
