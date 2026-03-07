package com.group5.hrm.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller

public class AuthController {

    @GetMapping("/login")
    public String login(){
        return "/auth/login";
    }
}
