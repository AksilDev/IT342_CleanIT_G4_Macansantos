package com.G4.backend.repository;

import com.G4.backend.entity.TechnicianSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TechnicianSettingsRepository extends JpaRepository<TechnicianSettings, UUID> {
    
}