package com.G4.backend.controller;

import com.G4.backend.entity.AddOn;
import com.G4.backend.entity.Booking;
import com.G4.backend.entity.BookingPhoto;
import com.G4.backend.repository.AddOnRepository;
import com.G4.backend.repository.BookingPhotoRepository;
import com.G4.backend.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/bookings")
@CrossOrigin(origins = "http://localhost:5173")
public class BookingController {

    private final BookingService bookingService;
    private final AddOnRepository addOnRepository;
    private final BookingPhotoRepository bookingPhotoRepository;

    public BookingController(BookingService bookingService, 
                            AddOnRepository addOnRepository,
                            BookingPhotoRepository bookingPhotoRepository) {
        this.bookingService = bookingService;
        this.addOnRepository = addOnRepository;
        this.bookingPhotoRepository = bookingPhotoRepository;
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
                
                // BUG FIX 1: Resolve addon IDs to formatted names "Name (₱Price)"
                List<String> formattedAddons = new ArrayList<>();
                if (booking.getAddOns() != null && !booking.getAddOns().isEmpty()) {
                    String[] addonIds = booking.getAddOns().split(",");
                    for (String addonId : addonIds) {
                        try {
                            UUID addonUUID = UUID.fromString(addonId.trim());
                            Optional<AddOn> addonOpt = addOnRepository.findById(addonUUID);
                            if (addonOpt.isPresent()) {
                                AddOn addon = addonOpt.get();
                                // Format as "Name (₱Price)"
                                formattedAddons.add(addon.getName() + " (₱" + String.format("%.0f", addon.getPrice()) + ")");
                            } else {
                                // Fallback to UUID if addon not found (log warning)
                                System.err.println("Warning: Addon not found for ID: " + addonId);
                                formattedAddons.add(addonId.trim());
                            }
                        } catch (IllegalArgumentException e) {
                            // Invalid UUID format, skip
                            System.err.println("Warning: Invalid addon UUID format: " + addonId);
                        }
                    }
                }
                map.put("addOns", formattedAddons);
                
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
                
                // BUG FIX 2: Include photos from booking_photos table
                List<BookingPhoto> photos = bookingPhotoRepository.findByBookingId(booking.getId());
                if (!photos.isEmpty()) {
                    List<Map<String, Object>> photosList = new ArrayList<>();
                    for (BookingPhoto photo : photos) {
                        Map<String, Object> photoMap = new HashMap<>();
                        photoMap.put("id", photo.getId());
                        photoMap.put("type", photo.getType().toString());
                        photoMap.put("fileUrl", photo.getFileUrl());
                        photoMap.put("uploadedAt", photo.getUploadedAt());
                        photosList.add(photoMap);
                    }
                    map.put("photos", photosList);
                } else {
                    map.put("photos", new ArrayList<>()); // Empty array if no photos
                }
                
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

    @PostMapping("/{bookingId}/reschedule")
    public ResponseEntity<?> rescheduleBooking(
            @PathVariable UUID bookingId,
            @RequestBody Map<String, Object> request) {
        try {
            UUID requestedBy = UUID.fromString((String) request.get("requestedBy"));
            String newDateStr = (String) request.get("newBookingDate");
            String newTimeSlot = (String) request.get("newTimeSlot");
            String reason = (String) request.getOrDefault("reason", "Rescheduled by client");

            java.time.LocalDate newDate = java.time.LocalDate.parse(newDateStr);

            Booking rescheduledBooking = bookingService.rescheduleBooking(
                bookingId, newDate, newTimeSlot, requestedBy, reason
            );

            return ResponseEntity.ok(Map.of(
                "message", "Booking rescheduled successfully",
                "bookingId", rescheduledBooking.getId(),
                "newDate", rescheduledBooking.getBookingDate(),
                "newTimeSlot", rescheduledBooking.getTimeSlot(),
                "status", rescheduledBooking.getStatus().getValue()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to reschedule booking",
                "message", e.getMessage()
            ));
        }
    }
}
