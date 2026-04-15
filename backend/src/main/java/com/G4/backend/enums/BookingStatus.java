package com.G4.backend.enums;

/**
 * Enum representing the various states of a booking in the CleanIT system.
 * This follows the booking lifecycle from creation to completion/cancellation.
 */
public enum BookingStatus {
    PENDING("pending", "Booking created and awaiting technician acceptance"),
    CONFIRMED("confirmed", "Booking confirmed by technician"),
    IN_PROGRESS("in_progress", "Service is currently being performed"),
    COMPLETED("completed", "Service has been completed successfully"),
    CANCELLED("cancelled", "Booking has been cancelled"),
    NO_SHOW("no_show", "Client was not available at scheduled time");

    private final String value;
    private final String description;

    BookingStatus(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Get BookingStatus from string value
     */
    public static BookingStatus fromValue(String value) {
        for (BookingStatus status : BookingStatus.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid booking status: " + value);
    }

    /**
     * Check if transition from current status to new status is valid
     */
    public boolean canTransitionTo(BookingStatus newStatus) {
        switch (this) {
            case PENDING:
                return newStatus == CONFIRMED || newStatus == CANCELLED;
            case CONFIRMED:
                return newStatus == IN_PROGRESS || newStatus == NO_SHOW || newStatus == CANCELLED;
            case IN_PROGRESS:
                return newStatus == COMPLETED;
            case COMPLETED:
            case CANCELLED:
            case NO_SHOW:
                return false; // Terminal states
            default:
                return false;
        }
    }

    /**
     * Get all valid next statuses from current status
     */
    public BookingStatus[] getValidTransitions() {
        switch (this) {
            case PENDING:
                return new BookingStatus[]{CONFIRMED, CANCELLED};
            case CONFIRMED:
                return new BookingStatus[]{IN_PROGRESS, NO_SHOW, CANCELLED};
            case IN_PROGRESS:
                return new BookingStatus[]{COMPLETED};
            default:
                return new BookingStatus[0]; // Terminal states have no valid transitions
        }
    }
}