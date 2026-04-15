package com.G4.backend.controller;

import com.G4.backend.entity.Booking;
import com.G4.backend.entity.TechnicianSettings;
import com.G4.backend.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/technician")
@CrossOrigin(origins = "http://localhost:5173")
public class TechnicianBookingController {

    private final BookingService bookingService;

    public TechnicianBookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * Get all pending bookings that technicians can accept
     */
    @GetMapping("/bookings/pending")
    public ResponseEntity<?> getPendingBookings() {
        try {
            List<Map<String, Object>> bookings = bookingService.getPendingBookingsForTechnicians();
            return ResponseEntity.ok(bookings);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to fetch pending bookings",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Technician accepts a pending booking
     */
    @PostMapping("/bookings/{bookingId}/accept")
    public ResponseEntity<?> acceptBooking(
            @PathVariable UUID bookingId,
            @RequestBody Map<String, String> request) {
        try {
            UUID technicianId = UUID.fromString(request.get("technicianId"));
            Booking acceptedBooking = bookingService.acceptBooking(bookingId, technicianId);
            
            return ResponseEntity.ok(Map.of(
                "message", "Booking accepted successfully",
                "booking", acceptedBooking
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to accept booking",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Get technician's accepted bookings
     */
    @GetMapping("/{technicianId}/bookings")
    public ResponseEntity<?> getTechnicianBookings(@PathVariable UUID technicianId) {
        try {
            List<Booking> bookings = bookingService.getTechnicianBookings(technicianId);
            List<Map<String, Object>> response = new ArrayList<>();
            
            for (Booking booking : bookings) {
                Map<String, Object> bookingMap = new HashMap<>();
                bookingMap.put("id", booking.getId());
                bookingMap.put("bookingCode", booking.getBookingCode());
                bookingMap.put("serviceType", booking.getServiceType());
                bookingMap.put("deviceType", booking.getDeviceType());
                bookingMap.put("timeSlot", booking.getTimeSlot());
                bookingMap.put("bookingDate", booking.getBookingDate());
                bookingMap.put("status", booking.getStatus().getValue());
                bookingMap.put("totalAmount", booking.getTotalAmount());
                bookingMap.put("createdAt", booking.getCreatedAt());
                bookingMap.put("confirmedAt", booking.getConfirmedAt());
                
                // Show address only for confirmed bookings
                if (booking.getStatus() != com.G4.backend.enums.BookingStatus.PENDING) {
                    bookingMap.put("address", booking.getAddress());
                    bookingMap.put("landmark", booking.getLandmark());
                } else {
                    bookingMap.put("address", "Address will be visible after confirmation");
                    bookingMap.put("landmark", null);
                }
                
                response.add(bookingMap);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to fetch technician bookings",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Toggle technician availability
     */
    @PostMapping("/{technicianId}/availability")
    public ResponseEntity<?> toggleAvailability(
            @PathVariable UUID technicianId,
            @RequestBody Map<String, Boolean> request) {
        try {
            boolean isAvailable = request.get("isAvailable");
            TechnicianSettings settings = bookingService.toggleTechnicianAvailability(technicianId, isAvailable);
            
            return ResponseEntity.ok(Map.of(
                "message", "Availability updated successfully",
                "isAvailable", settings.getIsAvailable(),
                "updatedAt", settings.getUpdatedAt()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to update availability",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Get technician availability status
     */
    @GetMapping("/{technicianId}/availability")
    public ResponseEntity<?> getAvailability(@PathVariable UUID technicianId) {
        try {
            boolean isAvailable = bookingService.getTechnicianAvailability(technicianId);
            return ResponseEntity.ok(Map.of("isAvailable", isAvailable));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to fetch availability",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Update booking status (for technicians to mark progress)
     */
    @PostMapping("/bookings/{bookingId}/status")
    public ResponseEntity<?> updateBookingStatus(
            @PathVariable UUID bookingId,
            @RequestBody Map<String, String> request) {
        try {
            String statusValue = request.get("status");
            UUID technicianId = UUID.fromString(request.get("technicianId"));
            String reason = request.getOrDefault("reason", "Status updated by technician");
            
            com.G4.backend.enums.BookingStatus newStatus = 
                com.G4.backend.enums.BookingStatus.fromValue(statusValue);
            
            com.G4.backend.entity.Booking updatedBooking = 
                bookingService.updateBookingStatus(bookingId, newStatus, technicianId, reason);
            
            return ResponseEntity.ok(Map.of(
                "message", "Booking status updated successfully",
                "bookingId", updatedBooking.getId(),
                "status", updatedBooking.getStatus().getValue(),
                "updatedAt", updatedBooking.getUpdatedAt()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to update booking status",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Get technician's booking statistics
     */
    @GetMapping("/{technicianId}/statistics")
    public ResponseEntity<?> getTechnicianStatistics(@PathVariable UUID technicianId) {
        try {
            List<com.G4.backend.entity.Booking> allBookings = bookingService.getTechnicianBookings(technicianId);
            
            Map<String, Object> stats = new HashMap<>();
            
            // Count by status
            long completed = allBookings.stream()
                .mapToLong(b -> b.getStatus() == com.G4.backend.enums.BookingStatus.COMPLETED ? 1 : 0)
                .sum();
            long confirmed = allBookings.stream()
                .mapToLong(b -> b.getStatus() == com.G4.backend.enums.BookingStatus.CONFIRMED ? 1 : 0)
                .sum();
            long inProgress = allBookings.stream()
                .mapToLong(b -> b.getStatus() == com.G4.backend.enums.BookingStatus.IN_PROGRESS ? 1 : 0)
                .sum();
            
            stats.put("totalBookings", allBookings.size());
            stats.put("completed", completed);
            stats.put("confirmed", confirmed);
            stats.put("inProgress", inProgress);
            stats.put("active", confirmed + inProgress);
            
            // Calculate total earnings from completed bookings
            double totalEarnings = allBookings.stream()
                .filter(b -> b.getStatus() == com.G4.backend.enums.BookingStatus.COMPLETED)
                .mapToDouble(b -> b.getTotalAmount())
                .sum();
            stats.put("totalEarnings", totalEarnings);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to fetch technician statistics",
                "message", e.getMessage()
            ));
        }
    }
}