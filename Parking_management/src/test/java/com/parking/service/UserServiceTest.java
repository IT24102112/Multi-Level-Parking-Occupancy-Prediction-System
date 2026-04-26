package com.parking.service;

import com.parking.model.AppUser;
import com.parking.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private UserService userService;

    private AppUser testUser;

    @BeforeEach
    void setUp() {
        testUser = new AppUser();
        testUser.setUsername("testuser");
        testUser.setPassword("password");
        testUser.setFullName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setEnabled(true);
    }

    @Test
    void usernameExists_Existing_ReturnsTrue() {
        when(appUserRepository.existsById("testuser")).thenReturn(true);
        assertTrue(userService.usernameExists("testuser"));
    }

    @Test
    void emailExists_Existing_ReturnsTrue() {
        when(appUserRepository.existsByEmail("test@example.com")).thenReturn(true);
        assertTrue(userService.emailExists("test@example.com"));
    }

    @Test
    void registerNewUser_InsertsUserAndRole() {
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        // Mock jdbcTemplate.update() to return 1 (rows affected)
        when(jdbcTemplate.update(anyString(), anyString(), anyString(), anyBoolean(), anyString(), anyString(), anyBoolean()))
                .thenReturn(1);
        when(jdbcTemplate.update(anyString(), anyString(), anyString()))
                .thenReturn(1);

        userService.registerNewUser(testUser);

        // Verify user insertion
        verify(jdbcTemplate, times(1)).update(
                eq("INSERT INTO users (username, password, enabled, full_name, email, is_blacklisted) VALUES (?, ?, ?, ?, ?, ?)"),
                eq("testuser"), eq("encoded"), eq(true), eq("Test User"), eq("test@example.com"), eq(false)
        );
        // Verify role insertion
        verify(jdbcTemplate, times(1)).update(
                eq("INSERT INTO authorities (username, authority) VALUES (?, ?)"),
                eq("testuser"), eq("ROLE_USER")
        );
    }

    @Test
    void findByUsername_Existing_ReturnsUser() {
        when(appUserRepository.findById("testuser")).thenReturn(Optional.of(testUser));
        AppUser found = userService.findByUsername("testuser");
        assertNotNull(found);
        assertEquals("testuser", found.getUsername());
    }

    @Test
    void subscribeToPlan_UserNotBlacklisted_SetsPlan() {
        when(appUserRepository.findById("testuser")).thenReturn(Optional.of(testUser));
        when(appUserRepository.save(any())).thenReturn(testUser);

        userService.subscribeToPlan("testuser", "Weekly");

        assertNotNull(testUser.getPlanType());
        assertEquals("Weekly", testUser.getPlanType());
        assertNotNull(testUser.getPlanStartDate());
        assertNotNull(testUser.getPlanEndDate());
        assertTrue(testUser.getPlanEndDate().isAfter(testUser.getPlanStartDate()));
    }

    @Test
    void unsubscribe_RemovesPlan() {
        testUser.setPlanType("Weekly");
        testUser.setPlanStartDate(LocalDateTime.now());
        testUser.setPlanEndDate(LocalDateTime.now().plusWeeks(1));
        when(appUserRepository.findById("testuser")).thenReturn(Optional.of(testUser));
        when(appUserRepository.save(any())).thenReturn(testUser);

        userService.unsubscribe("testuser");

        assertNull(testUser.getPlanType());
        assertNull(testUser.getPlanStartDate());
        assertNull(testUser.getPlanEndDate());
    }

    @Test
    void removeExpiredPlans_ExpiredPlan_Removes() {
        testUser.setPlanType("Weekly");
        testUser.setPlanStartDate(LocalDateTime.now().minusWeeks(2));
        testUser.setPlanEndDate(LocalDateTime.now().minusWeeks(1)); // expired
        when(appUserRepository.findById("testuser")).thenReturn(Optional.of(testUser));
        when(appUserRepository.save(any())).thenReturn(testUser);

        boolean removed = userService.removeExpiredPlans("testuser");
        assertTrue(removed);
        assertNull(testUser.getPlanType());
    }

    @Test
    void updateProfile_EmailChangedAndUnique_Updates() {
        when(appUserRepository.findById("testuser")).thenReturn(Optional.of(testUser));
        when(appUserRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(appUserRepository.save(any())).thenReturn(testUser);

        boolean updated = userService.updateProfile("testuser", "New Name", "new@example.com");
        assertTrue(updated);
        assertEquals("New Name", testUser.getFullName());
        assertEquals("new@example.com", testUser.getEmail());
    }

    @Test
    void updateProfile_EmailDuplicate_Fails() {
        when(appUserRepository.findById("testuser")).thenReturn(Optional.of(testUser));
        when(appUserRepository.existsByEmail("duplicate@example.com")).thenReturn(true);

        boolean updated = userService.updateProfile("testuser", "New Name", "duplicate@example.com");
        assertFalse(updated);
        verify(appUserRepository, never()).save(any());
    }

    @Test
    void deleteAccount_RemovesUserAndAuthorities() {
        // Mock jdbcTemplate.update() to return 1
        when(jdbcTemplate.update(eq("DELETE FROM authorities WHERE username = ?"), anyString()))
                .thenReturn(1);
        when(jdbcTemplate.update(eq("DELETE FROM users WHERE username = ?"), anyString()))
                .thenReturn(1);

        userService.deleteAccount("testuser");

        verify(jdbcTemplate, times(1)).update("DELETE FROM authorities WHERE username = ?", "testuser");
        verify(jdbcTemplate, times(1)).update("DELETE FROM users WHERE username = ?", "testuser");
    }

    @Test
    void blacklistUser_SetsEnabledFalse() {
        // Mock jdbcTemplate.update() to return 1
        when(jdbcTemplate.update(eq("UPDATE users SET enabled = 0, is_blacklisted = 1 WHERE username = ?"), anyString()))
                .thenReturn(1);

        userService.blacklistUser("testuser");

        verify(jdbcTemplate, times(1)).update(
                eq("UPDATE users SET enabled = 0, is_blacklisted = 1 WHERE username = ?"),
                eq("testuser")
        );
    }

    @Test
    void unblacklistUser_SetsEnabledTrue() {
        // Mock jdbcTemplate.update() to return 1
        when(jdbcTemplate.update(eq("UPDATE users SET enabled = 1, is_blacklisted = 0 WHERE username = ?"), anyString()))
                .thenReturn(1);

        userService.unblacklistUser("testuser");

        verify(jdbcTemplate, times(1)).update(
                eq("UPDATE users SET enabled = 1, is_blacklisted = 0 WHERE username = ?"),
                eq("testuser")
        );
    }
}