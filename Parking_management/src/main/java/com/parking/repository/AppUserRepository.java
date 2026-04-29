package com.parking.repository;

import com.parking.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface AppUserRepository extends JpaRepository<AppUser, String> {
    boolean existsByEmail(String email);
    AppUser findByEmail(String email);

    @Query(value = "SELECT u.* FROM users u JOIN authorities a ON u.username = a.username WHERE a.authority = 'ROLE_USER'", nativeQuery = true)
    List<AppUser> findAllUsers();

    @Query("SELECT u FROM AppUser u WHERE u.planEndDate IS NOT NULL AND u.planStartDate IS NOT NULL AND u.planEndDate > CURRENT_TIMESTAMP")
    List<AppUser> findActiveUsers();


}