package com.G4.backend.controller;

import com.G4.backend.entity.Booking;
import com.G4.backend.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/bookings")
@CrossOrigin(origins = "http://localhost:5173")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createBooking(@RequestBody Map<String, Object> request) {
        try {
            Booking booking = bookingService.createBooking(request);
            
            // Return booking details without sensitive information
            Map<String, Object> response = new HashMap<>();
            response.put("id", booking.getId());
            response.put("bookingCode", booking.getBookingCode());
            response.put("serviceType", booking.getServiceType());
            response.put("deviceType", booking.getDeviceType());
            response.put("timeSlot", booking.getTimeSlot());
            response.put("bookingDate", booking.getBookingDate());
            response.put("totalAmount", booking.getTotalAmount());
            response.put("status", booking.getStatus().getValue());
            response.put("statusDescription", booking.getStatus().getDescription());
            response.put("createdAt", booking.getCreatedAt());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to create booking",
                "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<?> getClientBookings(@PathVariable UUID clientId) {
        try {
            List<Booking> bookings = bookingService.getClientBookings(clientId);
            List<Map<String, Object>> response = new ArrayList<>();
            
            for (Booking booking : bookings) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", booking.getId());
                map.put("bookingCode", booking.getBookingCode());
                map.put("serviceType", booking.getServiceType());
                map.put("deviceType", booking.getDeviceType());
                map.put("addOns", booking.getAddOns() != null ? 
                    Arrays.asList(booking.getAddOns().split(",")) : new ArrayList<>());
                map.put("timeSlot", booking.getTimeSlot());
                map.put("bookingDate", booking.getBookingDate());
                map.put("address", booking.getAddress()); // Client can always see their address
                map.put("landmark", booking.getLandmark());
                map.put("specialInstructions", booking.getSpecialInstructions());
                map.put("totalAmount", booking.getTotalAmount());
                map.put("status", booking.getStatus().getValue());
                map.put("statusDescription", booking.getStatus().getDescription());
                map.put("createdAt", booking.getCreatedAt());
                map.put("confirmedAt", booking.getConfirmedAt());
                map.put("startedAt", booking.getStartedAt());
                map.put("completedAt", booking.getCompletedAt());
                
                // Show technician acceptance status
                if (booking.getTechnicianId() != null) {
                    map.put("technicianAssigned", true);
                    map.put("technicianId", booking.getTechnicianId());
                    
                    // Get technician details from UserRepository
                    // For now, we'll add a placeholder - in real implementation you'd fetch from UserRepository
                    map.put("technicianAcceptedAt", booking.getConfirmedAt());
                    map.put("acceptanceMessage", "Your booking has been accepted by a technician!");
                } else {
                    map.put("technicianAssigned", false);
                    map.put("acceptanceMessage", "Waiting for technician to accept your booking...");
                }
                
                // Add status-specific messages for client
                switch (booking.getStatus()) {
                    case PENDING:
                        map.put("statusMessage", "Your booking is waiting for a technician to accept it.");
                        break;
                    case CONFIRMED:
                        map.put("statusMessage", "Great! A technician has accepted your booking.");
                        break;
                    case IN_PROGRESS:
                        map.put("statusMessage", "Your service is currently in progress.");
                        break;
                    case COMPLETED:
                        map.put("statusMessage", "Your service has been completed successfully!");
                        break;
                    case CANCELLED:
                        map.put("statusMessage", "This booking has been cancelled.");
                        break;
                    case NO_SHOW:
                        map.put("statusMessage", "Marked as no-show - please contact support.");
                        break;
                }
                
                response.add(map);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to fetch client bookings",
                "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<?> getBookingDetails(
            @PathVariable UUID bookingId,
            @RequestParam UUID requestingUserId) {
        try {
            Map<String, Object> booking = bookingService.getBookingDetails(bookingId, requestingUserId);
            return ResponseEntity.ok(booking);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to fetch booking details",
                "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<?> cancelBooking(
            @PathVariable UUID bookingId,
            @RequestBody Map<String, String> request) {
        try {
            UUID userId = UUID.fromString(request.get("userId"));
            String reason = request.getOrDefault("reason", "Cancelled by user");
            
            Booking cancelledBooking = bookingService.cancelBooking(bookingId, userId, reason);
            
            return ResponseEntity.ok(Map.of(
                "message", "Booking cancelled successfully",
                "bookingId", cancelledBooking.getId(),
                "status", cancelledBooking.getStatus().getValue()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to cancel booking",
                "message", e.getMessage()
            ));
        }
    }
}
