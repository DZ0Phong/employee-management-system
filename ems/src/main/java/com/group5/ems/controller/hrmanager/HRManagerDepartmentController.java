package com.group5.ems.controller.hrmanager;

import com.group5.ems.dto.response.UserDTO;
import com.group5.ems.service.hrmanager.HRManagerDepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/hr-manager")
@RequiredArgsConstructor
public class HRManagerDepartmentController {

    private final HRManagerDepartmentService departmentService;

    @GetMapping("/departments")
    public String viewDepartments(Model model) {
        model.addAllAttributes(departmentService.getDepartmentsViewData());
        return "hrmanager/departments";
    }

    @GetMapping("/departments/{id}/members")
    @ResponseBody
    public List<UserDTO> getDepartmentMembers(@PathVariable Long id) {
        return departmentService.getDepartmentMembers(id);
    }

    @GetMapping("/staffing-requests")
    public String viewStaffingRequests(Model model) {
        model.addAllAttributes(departmentService.getStaffingRequestsData());
        return "hrmanager/staffing_requests";
    }

    @PostMapping("/staffing-requests/{requestId}/assign")
    public String assignEmployee(@PathVariable Long requestId,
                                 @RequestParam Long employeeId,
                                 RedirectAttributes ra) {
        boolean success = departmentService.assignEmployeeToDepartment(requestId, employeeId);
        if (success) {
            ra.addFlashAttribute("successMsg", "Employee assigned successfully");
        } else {
            ra.addFlashAttribute("errorMsg", "Failed to assign employee");
        }
        return "redirect:/hr-manager/staffing-requests";
    }

    @PostMapping("/staffing-requests/{requestId}/reject")
    public String rejectRequest(@PathVariable Long requestId,
                               @RequestParam(required = false) String reason,
                               RedirectAttributes ra) {
        boolean success = departmentService.rejectStaffingRequest(requestId, reason);
        if (success) {
            ra.addFlashAttribute("successMsg", "Request rejected");
        } else {
            ra.addFlashAttribute("errorMsg", "Failed to reject request");
        }
        return "redirect:/hr-manager/staffing-requests";
    }

    @PostMapping("/employees/{employeeId}/transfer")
    public String transferEmployee(@PathVariable Long employeeId,
                                   @RequestParam Long fromDepartmentId,
                                   @RequestParam Long toDepartmentId,
                                   RedirectAttributes ra) {
        boolean success = departmentService.transferEmployeeBetweenDepartments(
                employeeId, fromDepartmentId, toDepartmentId);
        if (success) {
            ra.addFlashAttribute("successMsg", "Employee transferred successfully");
        } else {
            ra.addFlashAttribute("errorMsg", "Failed to transfer employee");
        }
        return "redirect:/hr-manager/staffing-requests";
    }
}
