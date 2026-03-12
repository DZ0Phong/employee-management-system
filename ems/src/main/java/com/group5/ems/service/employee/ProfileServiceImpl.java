package com.group5.ems.service.employee;

import com.group5.ems.dto.request.UpdateProfileRequest;
import com.group5.ems.dto.response.EmployeeProfileDTO;
import com.group5.ems.entity.Employee;
import com.group5.ems.entity.User;
import com.group5.ems.repository.EmployeeRepository;
import com.group5.ems.repository.UserRepository;
import com.group5.ems.service.employee.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public EmployeeProfileDTO getProfile(Long employeeId, Long userId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String departmentName = (employee.getDepartment() != null)
                ? employee.getDepartment().getName() : "";

        String positionName = (employee.getPosition() != null)
                ? employee.getPosition().getName() : "";

        return EmployeeProfileDTO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus())
                .employeeId(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .hireDate(employee.getHireDate())
                .departmentName(departmentName)
                .positionName(positionName)
                .build();
    }

    @Override
    @Transactional
    public void updateProfile(Long userId, UpdateProfileRequest dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (dto.getFullName() != null && !dto.getFullName().isBlank()) {
            user.setFullName(dto.getFullName());
        }
        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone());
        }
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            user.setEmail(dto.getEmail());
        }
        if (dto.getAvatarUrl() != null && !dto.getAvatarUrl().isBlank()) {
            user.setAvatarUrl(dto.getAvatarUrl());
        }

        userRepository.save(user);
    }
}