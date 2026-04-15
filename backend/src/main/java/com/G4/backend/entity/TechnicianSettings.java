package com.G4.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "technician_settings")
public class TechnicianSettings {

    @Id
    @Column(name = "technician_id")
    private UUID technicianId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "technician_id")
    private User technician;

    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable = true;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public TechnicianSettings() {}

    public TechnicianSettings(UUID technicianId, Boolean isAvailable) {
        this.technicianId = technicianId;
        this.isAvailable = isAvailable;
    }

    // Getters and Setters
    public UUID getTechnicianId() {
        return technicianId;
    }

    public void setTechnicianId(UUID technicianId) {
        this.technicianId = technicianId;
    }

    public User getTechnician() {
        return technician;
    }

    public void setTechnician(User technician) {
        this.technician = technician;
    }

    public Boolean getIsAvailable() {
        return isAvailable;
    }

    public void setIsAvailable(Boolean isAvailable) {
        this.isAvailable = isAvailable;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}