package com.group5.ems.controller.hrmanager;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/hrmanager")
public class HrManagerController {
    
    @GetMapping({"", "/", "/dashboard"})
    public String index() {
        return "hrmanager/dashboard";
    }
}
