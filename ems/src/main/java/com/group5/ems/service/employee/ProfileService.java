package com.group5.ems.service.employee;

import com.group5.ems.dto.request.UpdateProfileRequest;
import com.group5.ems.dto.response.EmployeeProfileDTO;

public interface ProfileService {
    EmployeeProfileDTO getProfile(Long employeeId, Long userId);
    void updateProfile(Long userId, UpdateProfileRequest dto);
}