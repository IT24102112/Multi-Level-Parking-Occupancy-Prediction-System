package com.parking.controller;

import com.parking.model.AppUser;
import com.parking.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('IT_ADMIN')")
public class AdminUserController {

    @Autowired
    private UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String listUsers(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        return "admin/users";
    }

    @PostMapping("/blacklist/{username}")
    public String blacklistUser(@PathVariable String username) {
        userService.blacklistUser(username);
        return "redirect:/admin/users";
    }

    @PostMapping("/unblacklist/{username}")
    public String unblacklistUser(@PathVariable String username) {
        userService.unblacklistUser(username);
        return "redirect:/admin/users";
    }
}