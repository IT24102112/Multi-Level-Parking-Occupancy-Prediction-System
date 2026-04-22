package com.parking.service;

import com.parking.model.AppUser;
import com.parking.model.PasswordResetToken;
import com.parking.repository.AppUserRepository;
import com.parking.repository.PasswordResetTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class UserService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

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

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

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



    @Value("${app.base.url}")
    private String baseUrl;

    // Generate a unique token
    private String generateToken() {
        return UUID.randomUUID().toString();
    }

    // Send verification email after registration
    @Transactional
    public void sendVerificationEmail(AppUser user) {
        String token = generateToken();
        LocalDateTime expiry = LocalDateTime.now().plusHours(24);

        // Update the user record using JdbcTemplate (consistent with registration)
        int updated = jdbcTemplate.update(
                "UPDATE users SET verification_token = ?, verification_token_expiry = ? WHERE username = ?",
                token, expiry, user.getUsername()
        );

        if (updated == 0) {
            logger.error("Failed to update verification token for user: {}", user.getUsername());
            return;
        }

        // Update the in-memory user object (optional, for later use)
        user.setVerificationToken(token);
        user.setVerificationTokenExpiry(expiry);

        // Send email
        String verificationUrl = baseUrl + "/verify?token=" + token;
        String subject = "Verify your email address";
        String content = "Dear " + user.getFullName() + ",\n\nPlease click the link below to verify your email address:\n" + verificationUrl + "\n\nThis link expires in 24 hours.";
        sendEmail(user.getEmail(), subject, content);

        logger.info("Verification email sent to {} with token: {}", user.getEmail(), token);
    }

    @Transactional
    public boolean verifyEmail(String token) {
        logger.info("Verifying token: '{}'", token);

        // Fetch user by token using JdbcTemplate
        String sql = "SELECT username, verification_token_expiry FROM users WHERE verification_token = ?";
        try {
            Map<String, Object> result = jdbcTemplate.queryForMap(sql, token);
            String username = (String) result.get("username");
            LocalDateTime expiry = ((java.sql.Timestamp) result.get("verification_token_expiry")).toLocalDateTime();

            if (expiry.isBefore(LocalDateTime.now())) {
                logger.warn("Token expired for user: {}", username);
                return false;
            }

            // Mark email as verified and clear token
            jdbcTemplate.update(
                    "UPDATE users SET email_verified = 1, verification_token = NULL, verification_token_expiry = NULL WHERE username = ?",
                    username
            );
            logger.info("Email verified successfully for user: {}", username);
            return true;
        } catch (Exception e) {
            logger.error("Token not found or error: {}", e.getMessage());
            return false;
        }
    }



    // Initiate password reset
    @Transactional
    public boolean initiatePasswordReset(String email) {
        AppUser user = appUserRepository.findByEmail(email);
        if (user == null) return false;
        // Delete any existing token for this user
        passwordResetTokenRepository.deleteByUsername(user.getUsername());

        String token = generateToken();
        PasswordResetToken resetToken = new PasswordResetToken(user.getUsername(), token, LocalDateTime.now().plusHours(1));
        passwordResetTokenRepository.save(resetToken);

        String resetUrl = baseUrl + "/reset-password?token=" + token;
        String subject = "Password reset request";
        String content = "Dear " + user.getFullName() + ",\n\nClick the link below to reset your password:\n" + resetUrl + "\n\nThis link expires in 1 hour.";
        sendEmail(user.getEmail(), subject, content);
        return true;
    }

    // Reset password
    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        Optional<PasswordResetToken> opt = passwordResetTokenRepository.findByToken(token);
        if (opt.isEmpty() || opt.get().getExpiryDate().isBefore(LocalDateTime.now())) {
            return false;
        }
        PasswordResetToken resetToken = opt.get();
        AppUser user = findByUsername(resetToken.getUsername());
        if (user == null) return false;
        user.setPassword(passwordEncoder.encode(newPassword));
        appUserRepository.save(user);
        passwordResetTokenRepository.deleteById(resetToken.getId());
        return true;
    }

    // Helper to send email
    private void sendEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
}