package com.group5.ems.controller.hr;

import com.group5.ems.dto.response.UserDTO;
import com.group5.ems.repository.RequestRepository;
import com.group5.ems.service.admin.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Arrays;

/**
 * Global ControllerAdvice for the HR module.
 * Provides common model attributes required by the HR sidebar and layout across all `/hr/**` views,
 * preventing duplicate retrieval and ensuring accurate UI notification metrics (like pending counts).
 */
@ControllerAdvice(basePackages = "com.group5.ems.controller.hr")
@RequiredArgsConstructor
public class HrGlobalControllerAdvice {

    private final AdminService adminService;
    private final RequestRepository requestRepository;

    @ModelAttribute("currentUser")
    public UserDTO currentUser() {
        return adminService.getUserDTO().orElse(null);
    }

    @ModelAttribute("pendingLeave")
    public int pendingLeave() {
        long count = requestRepository.countByStatusAndStepWaitingHRAndRequestTypeCodeIn(
                "PENDING", 
                Arrays.asList("LV_ANNUAL", "LV_SICK", "LEAVE_ANNUAL", "LEAVE_SICK", "LEAVE_UNPAID")
        );
        return (int) count;
    }

    @ModelAttribute("pendingRequests")
    public int pendingRequests() {
        return (int) requestRepository.countPendingWorkflowRequests();
    }
}
