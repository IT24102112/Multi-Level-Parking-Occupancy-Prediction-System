package com.parking.controller;

import com.parking.model.AppUser;
import com.parking.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        boolean planExpired = userService.removeExpiredPlans(username);
        AppUser user = userService.findByUsername(username);
        model.addAttribute("user", user);
        model.addAttribute("plans", userService.getAvailablePlans());
        if (planExpired) {
            model.addAttribute("message", "Your subscription has expired.");
        }
        return "user/dashboard";
    }

    @PostMapping("/subscribe")
    public String subscribe(@RequestParam String planType, RedirectAttributes redirectAttributes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        userService.subscribeToPlan(username, planType);
        redirectAttributes.addFlashAttribute("message", "Subscribed to " + planType + " plan successfully!");
        return "redirect:/user/dashboard";
    }

    @PostMapping("/unsubscribe")
    public String unsubscribe(RedirectAttributes redirectAttributes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        userService.unsubscribe(username);
        redirectAttributes.addFlashAttribute("message", "You have unsubscribed from your plan.");
        return "redirect:/user/dashboard";
    }

    // Edit profile form
    @GetMapping("/edit-profile")
    public String editProfileForm(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        AppUser user = userService.findByUsername(username);
        model.addAttribute("user", user);
        return "user/edit-profile";
    }

    // Update profile
    @PostMapping("/update-profile")
    public String updateProfile(@RequestParam String fullName, @RequestParam String email,
                                RedirectAttributes redirectAttributes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        boolean updated = userService.updateProfile(username, fullName, email);
        if (updated) {
            redirectAttributes.addFlashAttribute("message", "Profile updated successfully!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Email already in use or update failed.");
        }
        return "redirect:/user/dashboard";
    }

    // Delete account confirmation
    @GetMapping("/delete-account")
    public String deleteAccountConfirmation() {
        return "user/delete-account";
    }

    // Delete account
    @PostMapping("/delete-account")
    public String deleteAccount(RedirectAttributes redirectAttributes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        userService.deleteAccount(username);
        // Logout after deletion
        SecurityContextHolder.clearContext();
        redirectAttributes.addFlashAttribute("message", "Your account has been deleted.");
        return "redirect:/";
    }
}