package com.group5.ems.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @GetMapping("/dashboard")
    @ResponseBody
    public String dashboard() {
        return "Admin dashboard - OK (role ADMIN)";
    }

    @GetMapping("/users")
    public String users(Model model) {

        return "Admin users - OK (role USER)";
    }
}
