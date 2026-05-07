package com.G4.backend.controller;

import com.G4.backend.entity.User;
import com.G4.backend.repository.UserRepository;
import com.G4.backend.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/admin")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminController {

    private final BookingService bookingService;
    private final UserRepository userRepository;

    public AdminController(BookingService bookingService, UserRepository userRepository) {
        this.bookingService = bookingService;
        this.userRepository = userRepository;
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
     * Get bookings filtered by multiple statuses for admin drill-down
     * Used for statistics card drill-down functionality
     * 
     * @param statuses Comma-separated list of status values (e.g., "pending,confirmed,in_progress")
     * @return JSON response with bookings array, count, and statuses
     */
    @GetMapping("/bookings/by-status")
    public ResponseEntity<?> getBookingsByStatuses(@RequestParam String statuses) {
        try {
            // Parse comma-separated statuses into list
            String[] statusArray = statuses.split(",");
            List<com.G4.backend.enums.BookingStatus> statusList = new ArrayList<>();
            
            // Validate and convert each status string to BookingStatus enum
            for (String statusStr : statusArray) {
                try {
                    com.G4.backend.enums.BookingStatus bookingStatus = 
                        com.G4.backend.enums.BookingStatus.fromValue(statusStr.trim());
                    statusList.add(bookingStatus);
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body(Map.of(
                        "error", "Invalid status",
                        "message", "Unknown status: '" + statusStr.trim() + "'"
                    ));
                }
            }
            
            // Fetch bookings with complete details
            List<Map<String, Object>> bookings = bookingService.getBookingsByStatuses(statusList);
            
            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("bookings", bookings);
            response.put("count", bookings.size());
            response.put("statuses", statusArray);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to fetch bookings",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Void/Terminate a booking (admin only)
     * Cancels the booking with a reason indicating admin termination
     * 
     * @param bookingId The ID of the booking to void
     * @return Success message or error
     */
    @PostMapping("/bookings/{bookingId}/void")
    public ResponseEntity<?> voidBooking(@PathVariable UUID bookingId) {
        System.out.println("DEBUG: Void booking endpoint called for bookingId: " + bookingId);
        
        try {
            // Get current user email from Spring Security context
            org.springframework.security.core.Authentication authentication = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            
            System.out.println("DEBUG: Authentication object: " + authentication);
            System.out.println("DEBUG: Is authenticated: " + (authentication != null && authentication.isAuthenticated()));
            
            if (authentication == null || !authentication.isAuthenticated()) {
                System.out.println("ERROR: User not authenticated");
                return ResponseEntity.status(401).body(Map.of(
                    "error", "Unauthorized",
                    "message", "User not authenticated"
                ));
            }
            
            // Get the User object from the principal (set by JwtAuthenticationFilter)
            com.G4.backend.entity.User admin = (com.G4.backend.entity.User) authentication.getPrincipal();
            System.out.println("DEBUG: Admin email from authentication: " + admin.getEmail());
            
            System.out.println("DEBUG: Admin user found - ID: " + admin.getId() + ", Role: " + admin.getRole());
            
            // Verify user is admin
            if (!"admin".equals(admin.getRole())) {
                System.out.println("ERROR: User is not admin, role: " + admin.getRole());
                return ResponseEntity.status(403).body(Map.of(
                    "error", "Forbidden",
                    "message", "Only administrators can void bookings"
                ));
            }
            
            System.out.println("DEBUG: Calling cancelBooking with bookingId: " + bookingId + ", adminId: " + admin.getId());
            
            // Cancel the booking with admin termination reason
            String reason = "Booking voided/terminated by administrator";
            com.G4.backend.entity.Booking cancelledBooking = bookingService.cancelBooking(
                bookingId, 
                admin.getId(), 
                reason
            );
            
            System.out.println("DEBUG: Booking cancelled successfully - Code: " + cancelledBooking.getBookingCode());
            
            return ResponseEntity.ok(Map.of(
                "message", "Booking successfully voided",
                "bookingId", bookingId,
                "bookingCode", cancelledBooking.getBookingCode(),
                "status", "cancelled",
                "reason", reason
            ));
        } catch (com.G4.backend.exception.BookingException e) {
            System.out.println("ERROR: BookingException - " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Booking error",
                "message", e.getMessage(),
                "code", e.getErrorCode() != null ? e.getErrorCode() : "UNKNOWN"
            ));
        } catch (Exception e) {
            System.out.println("ERROR: Unexpected exception - " + e.getMessage());
            e.printStackTrace(); // Log the full error for debugging
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to void booking",
                "message", e.getMessage(),
                "type", e.getClass().getSimpleName()
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

    /**
     * Get users pending verification (unverified users)
     */
    @GetMapping("/pending-verifications")
    public ResponseEntity<?> getPendingVerifications() {
        try {
            List<String> roles = Arrays.asList("client", "technician");
            List<User> unverifiedUsers = userRepository.findPendingVerifications(roles);
            
            List<Map<String, Object>> response = new ArrayList<>();
            for (User user : unverifiedUsers) {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", user.getId());
                userMap.put("name", user.getName());
                userMap.put("email", user.getEmail());
                userMap.put("contactNo", user.getContactNo());
                userMap.put("role", user.getRole());
                userMap.put("imageUrl", user.getImageUrl());
                userMap.put("createdAt", user.getCreatedAt());
                userMap.put("verified", user.getVerified());
                response.add(userMap);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to fetch pending verifications",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Verify or reject a user
     */
    @PostMapping("/verify-user/{userId}")
    public ResponseEntity<?> verifyUser(
            @PathVariable UUID userId,
            @RequestBody Map<String, String> request) {
        try {
            String status = request.get("status");
            Optional<User> optionalUser = userRepository.findById(userId);
            
            if (optionalUser.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "User not found"
                ));
            }
            
            User user = optionalUser.get();
            
            if ("approved".equalsIgnoreCase(status)) {
                user.setVerified(true);
                userRepository.save(user);
                return ResponseEntity.ok(Map.of(
                    "message", "User approved successfully",
                    "userId", userId,
                    "status", "approved"
                ));
            } else if ("rejected".equalsIgnoreCase(status)) {
                // Delete the user if rejected
                userRepository.delete(user);
                return ResponseEntity.ok(Map.of(
                    "message", "User rejected and removed",
                    "userId", userId,
                    "status", "rejected"
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid status. Use 'approved' or 'rejected'"
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to verify user",
                "message", e.getMessage()
            ));
        }
    }
}