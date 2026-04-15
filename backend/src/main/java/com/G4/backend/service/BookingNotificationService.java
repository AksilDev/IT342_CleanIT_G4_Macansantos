package com.G4.backend.service;

import com.G4.backend.entity.Booking;
import com.G4.backend.entity.User;
import com.G4.backend.enums.BookingStatus;
import com.G4.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

/**
 * Service to handle booking-related notifications
 * This can be extended to send emails, SMS, or push notifications
 */
@Service
public class BookingNotificationService {

    private final UserRepository userRepository;

    public BookingNotificationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Send notification when booking status changes
     */
    public void notifyStatusChange(Booking booking, BookingStatus newStatus, String reason) {
        try {
            User client = userRepository.findById(booking.getClientId()).orElse(null);
            User technician = booking.getTechnicianId() != null ? 
                userRepository.findById(booking.getTechnicianId()).orElse(null) : null;

            switch (newStatus) {
                case PENDING -> notifyBookingCreated(booking, client);
                case CONFIRMED -> notifyBookingConfirmed(booking, client, technician);
                case IN_PROGRESS -> notifyServiceStarted(booking, client, technician);
                case COMPLETED -> notifyServiceCompleted(booking, client, technician);
                case CANCELLED -> notifyBookingCancelled(booking, client, technician, reason);
                case NO_SHOW -> notifyNoShow(booking, client, technician, reason);
            }
        } catch (Exception e) {
            // Log error but don't fail the booking operation
            System.err.println("Failed to send notification: " + e.getMessage());
        }
    }

    private void notifyBookingCreated(Booking booking, User client) {
        if (client != null) {
            sendNotification(client, 
                "Booking Created", 
                "Your booking for " + booking.getServiceType() + " service on " + booking.getBookingDate() + 
                " has been created and is awaiting technician acceptance."
            );
        }
    }

    private void notifyBookingConfirmed(Booking booking, User client, User technician) {
        if (client != null) {
            sendNotification(client, 
                "Booking Confirmed", 
                "Your booking for " + booking.getServiceType() + " service on " + booking.getBookingDate() + 
                " has been confirmed by the technician."
            );
        }
    }

    private void notifyServiceStarted(Booking booking, User client, User technician) {
        if (client != null) {
            sendNotification(client, 
                "Service Started", 
                "The technician has started working on your " + booking.getServiceType() + " service."
            );
        }
    }

    private void notifyServiceCompleted(Booking booking, User client, User technician) {
        if (client != null) {
            sendNotification(client, 
                "Service Completed", 
                "Your " + booking.getServiceType() + " service has been completed successfully. Thank you for choosing CleanIT!"
            );
        }
    }

    private void notifyBookingCancelled(Booking booking, User client, User technician, String reason) {
        String message = "The booking for " + booking.getServiceType() + " service on " + booking.getBookingDate() + 
            " has been cancelled.";
        if (reason != null && !reason.trim().isEmpty()) {
            message += " Reason: " + reason;
        }

        if (client != null) {
            sendNotification(client, "Booking Cancelled", message);
        }
        if (technician != null) {
            sendNotification(technician, "Booking Cancelled", message);
        }
    }

    private void notifyNoShow(Booking booking, User client, User technician, String reason) {
        if (client != null) {
            String message = "Your booking for " + booking.getServiceType() + " service on " + booking.getBookingDate() + 
                " was marked as no-show.";
            if (reason != null && !reason.trim().isEmpty()) {
                message += " Reason: " + reason;
            }
            sendNotification(client, "Booking No-Show", message);
        }
    }

    /**
     * Send notification to user
     * This is a placeholder implementation - can be extended to send actual emails/SMS
     */
    private void sendNotification(User user, String subject, String message) {
        // For now, just log the notification
        // In a real implementation, this would send email, SMS, or push notification
        System.out.println("NOTIFICATION - To: " + user.getName() + " (" + user.getEmail() + 
            "), Subject: " + subject + ", Message: " + message);
        
        // TODO: Implement actual notification sending
        // - Email service integration
        // - SMS service integration  
        // - Push notification service
        // - In-app notification storage
    }

    /**
     * Send notification to all available technicians about new booking
     */
    public void notifyTechniciansNewBooking(Booking booking) {
        try {
            User client = userRepository.findById(booking.getClientId()).orElse(null);
            String clientName = client != null ? client.getName() : "Unknown Client";
            
            // In a real implementation, this would find all available technicians and send notifications
            // For now, just log the notification
            System.out.println("TECHNICIAN NOTIFICATION - New booking available: " + booking.getServiceType() + 
                " service for " + clientName + " on " + booking.getBookingDate() + ". Booking ID: " + booking.getId());
        } catch (Exception e) {
            System.err.println("Failed to send technician notification: " + e.getMessage());
        }
    }
}