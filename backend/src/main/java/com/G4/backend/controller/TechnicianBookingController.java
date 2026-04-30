package com.G4.backend.controller;

import com.G4.backend.entity.Booking;
import com.G4.backend.entity.TechnicianSettings;
import com.G4.backend.service.BookingService;
import com.G4.backend.service.SupabaseStorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/v1/technician")
@CrossOrigin(origins = "http://localhost:5173")
public class TechnicianBookingController {

    private final BookingService bookingService;
    private final SupabaseStorageService storageService;

    public TechnicianBookingController(BookingService bookingService, SupabaseStorageService storageService) {
        this.bookingService = bookingService;
        this.storageService = storageService;
    }

    /**
     * Get all pending bookings that technicians can accept
     */
    @GetMapping("/bookings/pending")
    public ResponseEntity<?> getPendingBookings(@RequestParam("technicianId") String technicianIdStr) {
        try {
            UUID technicianId = UUID.fromString(technicianIdStr);
            List<Map<String, Object>> bookings = bookingService.getPendingBookingsForTechnicians(technicianId);
            return ResponseEntity.ok(bookings);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Invalid technician ID format",
                "message", e.getMessage()
            ));
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

    /**
     * Get booking checklist
     */
    @GetMapping("/bookings/{bookingId}/checklist")
    public ResponseEntity<?> getBookingChecklist(@PathVariable UUID bookingId) {
        try {
            List<Map<String, Object>> checklist = bookingService.getBookingChecklist(bookingId);
            return ResponseEntity.ok(checklist);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to fetch checklist",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Toggle checklist item
     */
    @PostMapping("/bookings/{bookingId}/checklist/{checklistItemId}")
    public ResponseEntity<?> toggleChecklistItem(
            @PathVariable UUID bookingId,
            @PathVariable UUID checklistItemId,
            @RequestBody Map<String, String> request) {
        try {
            UUID technicianId = UUID.fromString(request.get("technicianId"));
            var updatedItem = bookingService.toggleChecklistItem(bookingId, checklistItemId, technicianId);
            
            return ResponseEntity.ok(Map.of(
                "message", "Checklist item updated",
                "isChecked", updatedItem.getIsChecked(),
                "checkedAt", updatedItem.getCheckedAt()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to update checklist item",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Get booking photos
     */
    @GetMapping("/bookings/{bookingId}/photos")
    public ResponseEntity<?> getBookingPhotos(@PathVariable UUID bookingId) {
        try {
            List<Map<String, Object>> photos = bookingService.getBookingPhotos(bookingId);
            return ResponseEntity.ok(photos);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to fetch photos",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Validate checklist completion
     */
    @GetMapping("/bookings/{bookingId}/validate-checklist")
    public ResponseEntity<?> validateChecklist(@PathVariable UUID bookingId) {
        try {
            Map<String, Object> validation = bookingService.validateChecklistComplete(bookingId);
            return ResponseEntity.ok(validation);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to validate checklist",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Validate photo uploads
     */
    @GetMapping("/bookings/{bookingId}/validate-photos")
    public ResponseEntity<?> validatePhotos(@PathVariable UUID bookingId) {
        try {
            Map<String, Object> validation = bookingService.validatePhotosUploaded(bookingId);
            return ResponseEntity.ok(validation);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to validate photos",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Upload photo for booking (Before/After service)
     */
    @PostMapping("/bookings/{bookingId}/photos")
    public ResponseEntity<?> uploadPhoto(
            @PathVariable UUID bookingId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String type,
            @RequestParam("technicianId") String technicianId) {
        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "File is required"
                ));
            }

            // Validate type
            if (!type.equals("BEFORE") && !type.equals("AFTER")) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Photo type must be BEFORE or AFTER"
                ));
            }

            // Upload to Supabase
            String fileUrl = storageService.uploadFile(file, technicianId);

            // Save photo record
            bookingService.addBookingPhoto(bookingId, type, fileUrl, UUID.fromString(technicianId));

            return ResponseEntity.ok(Map.of(
                "message", "Photo uploaded successfully",
                "fileUrl", fileUrl
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to upload photo",
                "message", e.getMessage()
            ));
        }
    }
}