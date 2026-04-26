package com.parking.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class AppUser {

    @Id
    private String username;

    private String password;
    private boolean enabled;

    @Column(name = "full_name")
    private String fullName;

    private String email;

    @Column(name = "plan_type")
    private String planType;

    @Column(name = "plan_start_date")
    private LocalDateTime planStartDate;

    @Column(name = "plan_end_date")
    private LocalDateTime planEndDate;

    @Column(name = "is_blacklisted", nullable = false)
    private Boolean blacklisted = false;
    
    // Constructors
    public AppUser() {
        this.blacklisted = false;
    }

    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPlanType() { return planType; }
    public void setPlanType(String planType) { this.planType = planType; }

    public LocalDateTime getPlanStartDate() { return planStartDate; }
    public void setPlanStartDate(LocalDateTime planStartDate) { this.planStartDate = planStartDate; }

    public LocalDateTime getPlanEndDate() { return planEndDate; }
    public void setPlanEndDate(LocalDateTime planEndDate) { this.planEndDate = planEndDate; }

    public boolean isBlacklisted() {
        return Boolean.TRUE.equals(this.blacklisted);
    }
    public void setBlacklisted(Boolean blacklisted) {
        this.blacklisted = blacklisted != null ? blacklisted : false;
    }
}