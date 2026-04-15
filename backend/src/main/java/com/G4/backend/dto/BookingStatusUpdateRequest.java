package com.G4.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class BookingStatusUpdateRequest {
    
    @NotBlank(message = "Status is required")
    private String status;
    
    @NotNull(message = "Updated by user ID is required")
    private UUID updatedBy;
    
    private String reason;

    public BookingStatusUpdateRequest() {}

    public BookingStatusUpdateRequest(String status, UUID updatedBy, String reason) {
        this.status = status;
        this.updatedBy = updatedBy;
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}