package com.G4.backend.features.users;

import com.G4.backend.features.users.TechnicianSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TechnicianSettingsRepository extends JpaRepository<TechnicianSettings, UUID> {
    
}