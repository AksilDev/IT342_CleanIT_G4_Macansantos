package com.G4.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public class RescheduleBookingRequest {
    
    @NotNull(message = "New booking date is required")
    private LocalDate newBookingDate;
    
    @NotBlank(message = "New time slot is required")
    private String newTimeSlot;
    
    @NotNull(message = "Requested by user ID is required")
    private UUID requestedBy;
    
    private String reason;

    public RescheduleBookingRequest() {}

    public RescheduleBookingRequest(LocalDate newBookingDate, String newTimeSlot, UUID requestedBy, String reason) {
        this.newBookingDate = newBookingDate;
        this.newTimeSlot = newTimeSlot;
        this.requestedBy = requestedBy;
        this.reason = reason;
    }

    public LocalDate getNewBookingDate() {
        return newBookingDate;
    }

    public void setNewBookingDate(LocalDate newBookingDate) {
        this.newBookingDate = newBookingDate;
    }

    public String getNewTimeSlot() {
        return newTimeSlot;
    }

    public void setNewTimeSlot(String newTimeSlot) {
        this.newTimeSlot = newTimeSlot;
    }

    public UUID getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(UUID requestedBy) {
        this.requestedBy = requestedBy;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}