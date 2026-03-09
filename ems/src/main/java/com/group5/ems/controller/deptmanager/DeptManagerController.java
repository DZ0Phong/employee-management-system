package com.group5.ems.controller.deptmanager;

import com.group5.ems.service.deptmanager.DeptManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/dept-manager")
@RequiredArgsConstructor
public class DeptManagerController {

    private final DeptManagerService deptManagerService;

    @GetMapping({ "", "/", "/dashboard" })
    public String dashboard(Model model) {
        // Fetch mock data
        model.addAllAttributes(deptManagerService.getDashboardMockData());

        // Return the thymeleaf template
        return "deptmanager/index";
    }
}
