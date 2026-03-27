package com.G4.backend.repository;

import com.G4.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.role IN :roles AND (u.verified = false OR u.verified IS NULL)")
    List<User> findPendingVerifications(@Param("roles") List<String> roles);

    @Query("SELECT u FROM User u WHERE LOWER(u.role) = 'technician' AND (u.verified = true OR u.verified IS NULL) ORDER BY u.createdAt DESC LIMIT 5")
    List<User> findTop5VerifiedTechnicians();

    @Query("SELECT u FROM User u WHERE LOWER(u.role) = 'technician' AND (u.verified = true OR u.verified IS NULL) ORDER BY u.createdAt DESC")
    List<User> findAllVerifiedTechnicians();
}