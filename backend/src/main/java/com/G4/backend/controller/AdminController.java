package com.G4.backend.controller;

import com.G4.backend.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/admin")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminController {

    private final BookingService bookingService;

    public AdminController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * Get booking statistics for admin dashboard
     */
    @GetMapping("/dashboard/statistics")
    public ResponseEntity<?> getDashboardStatistics() {
        try {
            Map<String, Object> statistics = bookingService.getBookingStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to fetch dashboard statistics",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Get recent bookings for admin dashboard
     */
    @GetMapping("/dashboard/recent-bookings")
    public ResponseEntity<?> getRecentBookings(@RequestParam(defaultValue = "10") int limit) {
        try {
            List<Map<String, Object>> recentBookings = bookingService.getRecentBookings(limit);
            return ResponseEntity.ok(recentBookings);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to fetch recent bookings",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Get all bookings with pagination for admin management
     */
    @GetMapping("/bookings")
    public ResponseEntity<?> getAllBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            List<Map<String, Object>> bookings = bookingService.getAllBookingsForAdmin(page, size);
            
            Map<String, Object> response = new HashMap<>();
            response.put("bookings", bookings);
            response.put("page", page);
            response.put("size", size);
            response.put("hasMore", bookings.size() == size); // Simple check for more data
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to fetch bookings",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Get bookings by status for admin filtering
     */
    @GetMapping("/bookings/status/{status}")
    public ResponseEntity<?> getBookingsByStatus(@PathVariable String status) {
        try {
            com.G4.backend.enums.BookingStatus bookingStatus = 
                com.G4.backend.enums.BookingStatus.fromValue(status);
            
            List<com.G4.backend.entity.Booking> bookings = 
                bookingService.getBookingsByStatus(bookingStatus);
            
            List<Map<String, Object>> response = new ArrayList<>();
            for (com.G4.backend.entity.Booking booking : bookings) {
                Map<String, Object> bookingMap = new HashMap<>();
                bookingMap.put("id", booking.getId());
                bookingMap.put("bookingCode", booking.getBookingCode());
                bookingMap.put("serviceType", booking.getServiceType());
                bookingMap.put("deviceType", booking.getDeviceType());
                bookingMap.put("status", booking.getStatus().getValue());
                bookingMap.put("totalAmount", booking.getTotalAmount());
                bookingMap.put("bookingDate", booking.getBookingDate());
                bookingMap.put("timeSlot", booking.getTimeSlot());
                bookingMap.put("createdAt", booking.getCreatedAt());
                bookingMap.put("address", booking.getAddress()); // Admin sees all addresses
                
                response.add(bookingMap);
            }
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Invalid status",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to fetch bookings by status",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Get dashboard overview with key metrics
     */
    @GetMapping("/dashboard/overview")
    public ResponseEntity<?> getDashboardOverview() {
        try {
            Map<String, Object> statistics = bookingService.getBookingStatistics();
            List<Map<String, Object>> recentBookings = bookingService.getRecentBookings(5);
            
            Map<String, Object> overview = new HashMap<>();
            overview.put("statistics", statistics);
            overview.put("recentBookings", recentBookings);
            
            // Add some derived metrics
            long totalBookings = (Long) statistics.get("total");
            long completedBookings = (Long) statistics.get("completed");
            long pendingBookings = (Long) statistics.get("pending");
            long confirmedBookings = (Long) statistics.get("confirmed");
            
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("completionRate", totalBookings > 0 ? 
                Math.round((completedBookings * 100.0) / totalBookings) : 0);
            metrics.put("activeBookings", pendingBookings + confirmedBookings);
            metrics.put("totalRevenue", statistics.get("totalRevenue"));
            metrics.put("monthRevenue", statistics.get("monthRevenue"));
            
            overview.put("metrics", metrics);
            
            return ResponseEntity.ok(overview);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to fetch dashboard overview",
                "message", e.getMessage()
            ));
        }
    }
}