package com.group5.ems.controller.hrmanager;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/hrmanager")
public class HrManagerController {
    
    @GetMapping({"", "/", "/dashboard"})
    public String dashboard() {
        return "hrmanager/dashboard";
    }
    
    @GetMapping("/leave-approval")
    public String leaveApproval() {
        return "hrmanager/leave_approval";
    }
    
    @GetMapping("/payroll-approval")
    public String payrollApproval() {
        return "hrmanager/payroll_approval";
    }
    
    @GetMapping("/hr-analytics")
    public String hrAnalytics() {
        return "hrmanager/hr_analytics";
    }
}
