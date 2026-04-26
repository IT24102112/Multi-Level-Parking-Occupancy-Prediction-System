package com.parking.controller;

import com.parking.model.AppUser;
import com.parking.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;

@Controller
@RequestMapping("/register")
public class RegistrationController {

    @Autowired
    private UserService userService;

    @GetMapping
    public String showRegistrationForm(Model model) {
        // Add an empty user object to the model for the form
        if (!model.containsAttribute("user")) {
            model.addAttribute("user", new AppUser());
        }
        return "register";
    }

    @PostMapping
    public String registerUser(@Valid @ModelAttribute("user") AppUser user,
                               BindingResult result,
                               RedirectAttributes redirectAttributes) {
        // Custom validations (duplicate username/email)
        if (userService.usernameExists(user.getUsername())) {
            result.rejectValue("username", "error.user", "Username already exists");
        }
        if (userService.emailExists(user.getEmail())) {
            result.rejectValue("email", "error.user", "Email already registered");
        }

        if (result.hasErrors()) {
            // Return to form with errors; model already contains the user object
            return "register";
        }

        userService.registerNewUser(user);
        redirectAttributes.addFlashAttribute("success", "Registration successful! Please log in.");
        return "redirect:/login";
    }
}