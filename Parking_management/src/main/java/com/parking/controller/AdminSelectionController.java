package com.parking.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin")
public class AdminSelectionController {

    @GetMapping("/portal")
    public String selectAdmin() {
        return "admin/select";
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam String role, Model model) {
        model.addAttribute("selectedRole", role);
        return "admin/login";
    }
}