package com.group5.ems.controller.hrmanager;

import com.group5.ems.dto.response.UserDTO;
import com.group5.ems.service.admin.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(basePackages = "com.group5.ems.controller.hrmanager")
@RequiredArgsConstructor
public class HrManagerGlobalControllerAdvice {

    private final AdminService adminService;

    @ModelAttribute("currentUser")
    public UserDTO currentUser() {
        return adminService.getUserDTO().orElse(null);
    }
}
