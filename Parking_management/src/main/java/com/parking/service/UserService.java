package com.parking.service;

import com.parking.model.AppUser;
import com.parking.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public boolean usernameExists(String username) {
        return appUserRepository.existsById(username);
    }

    public boolean emailExists(String email) {
        return appUserRepository.existsByEmail(email);
    }

    @Transactional
    public void registerNewUser(AppUser user) {
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        jdbcTemplate.update(
                "INSERT INTO users (username, password, enabled, full_name, email, is_blacklisted) VALUES (?, ?, ?, ?, ?, ?)",
                user.getUsername(), encodedPassword, true, user.getFullName(), user.getEmail(), false
        );
        jdbcTemplate.update(
                "INSERT INTO authorities (username, authority) VALUES (?, ?)",
                user.getUsername(), "ROLE_USER"
        );
    }

    public AppUser findByUsername(String username) {
        return appUserRepository.findById(username).orElse(null);
    }

    public List<String> getAvailablePlans() {
        return Arrays.asList("Daily", "Weekly", "Monthly");
    }

    @Transactional
    public void subscribeToPlan(String username, String planType) {
        AppUser user = findByUsername(username);
        if (user != null && !user.isBlacklisted()) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime endDate = null;
            switch (planType) {
                case "Daily":
                    endDate = now.plusDays(1);
                    break;
                case "Weekly":
                    endDate = now.plusWeeks(1);
                    break;
                case "Monthly":
                    endDate = now.plusMonths(1);
                    break;
            }
            user.setPlanType(planType);
            user.setPlanStartDate(now);
            user.setPlanEndDate(endDate);
            appUserRepository.save(user);
        }
    }

    @Transactional
    public void unsubscribe(String username) {
        AppUser user = findByUsername(username);
        if (user != null && user.getPlanType() != null) {
            user.setPlanType(null);
            user.setPlanStartDate(null);
            user.setPlanEndDate(null);
            appUserRepository.save(user);
        }
    }

    @Transactional
    public boolean removeExpiredPlans(String username) {
        AppUser user = findByUsername(username);
        if (user != null && user.getPlanEndDate() != null && user.getPlanEndDate().isBefore(LocalDateTime.now())) {
            user.setPlanType(null);
            user.setPlanStartDate(null);
            user.setPlanEndDate(null);
            appUserRepository.save(user);
            return true;
        }
        return false;
    }

    // Profile update
    @Transactional
    public boolean updateProfile(String username, String fullName, String email) {
        AppUser user = findByUsername(username);
        if (user == null) return false;
        // Check email uniqueness
        if (!user.getEmail().equals(email) && emailExists(email)) {
            return false;
        }
        user.setFullName(fullName);
        user.setEmail(email);
        appUserRepository.save(user);
        return true;
    }

    // Delete account
    @Transactional
    public void deleteAccount(String username) {
        jdbcTemplate.update("DELETE FROM authorities WHERE username = ?", username);
        jdbcTemplate.update("DELETE FROM users WHERE username = ?", username);
    }

    // Blacklist (disable account)
    @Transactional
    public void blacklistUser(String username) {
        // Update both enabled (0) and is_blacklisted (1)
        jdbcTemplate.update("UPDATE users SET enabled = 0, is_blacklisted = 1 WHERE username = ?", username);
    }

    // Unblacklist (enable account)
    @Transactional
    public void unblacklistUser(String username) {
        // Update both enabled (1) and is_blacklisted (0)
        jdbcTemplate.update("UPDATE users SET enabled = 1, is_blacklisted = 0 WHERE username = ?", username);
    }

    // Get all users (for admin)
    public List<AppUser> getAllUsers() {
        List<AppUser> users = appUserRepository.findAll();
        if (users == null) return java.util.Collections.emptyList();
        return users.stream()
                .filter(u -> u.getUsername() != null && u.getEmail() != null)
                .collect(Collectors.toList());
    }


    public List<AppUser> getUsersWithActivePlans() {
        List<AppUser> activeUsers = appUserRepository.findActiveUsers();
        if (activeUsers == null) return java.util.Collections.emptyList();
        LocalDateTime now = LocalDateTime.now();
        return activeUsers.stream()
                .filter(u -> u.getPlanEndDate() != null && u.getPlanEndDate().isAfter(now))
                .collect(Collectors.toList());
    }

    public double calculateBillAmount(AppUser user) {
        if (user.getPlanType() == null) return 0;
        switch (user.getPlanType()) {
            case "Daily":
                return 600.00;
            case "Weekly":
                return 3500.00;
            case "Monthly":
                return 12000.00;
            default:
                return 0;
        }
    }
}