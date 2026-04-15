package com.G4.backend.service;

import com.G4.backend.entity.Booking;
import com.G4.backend.entity.TechnicianSettings;
import com.G4.backend.entity.User;
import com.G4.backend.enums.BookingStatus;
import com.G4.backend.exception.BookingException;
import com.G4.backend.repository.BookingRepository;
import com.G4.backend.repository.TechnicianSettingsRepository;
import com.G4.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final TechnicianSettingsRepository technicianSettingsRepository;
    private final BookingNotificationService notificationService;

    public BookingService(BookingRepository bookingRepository, UserRepository userRepository, 
                         TechnicianSettingsRepository technicianSettingsRepository,
                         BookingNotificationService notificationService) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.technicianSettingsRepository = technicianSettingsRepository;
        this.notificationService = notificationService;
    }

    /**
     * Create a new booking (no technician assigned initially)
     */
    public Booking createBooking(Map<String, Object> bookingData) {
        // Validate client exists and is verified
        UUID clientId = UUID.fromString(bookingData.get("clientId").toString());
        
        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new BookingException("Client not found", "CLIENT_NOT_FOUND"));

        // Validate user role and verification status
        if (!"client".equals(client.getRole())) {
            throw new BookingException("User is not a client", "INVALID_CLIENT_ROLE");
        }
        if (!client.isVerified()) {
            throw new BookingException("Client account is not verified", "CLIENT_NOT_VERIFIED");
        }

        Booking booking = new Booking();
        booking.setClientId(clientId);
        // No technician assigned initially - they will accept the booking
        booking.setServiceType(bookingData.get("serviceType").toString());
        booking.setDeviceType(bookingData.get("deviceType").toString());
        
        @SuppressWarnings("unchecked")
        List<String> addOns = (List<String>) bookingData.get("addOns");
        if (addOns != null && !addOns.isEmpty()) {
            booking.setAddOns(String.join(",", addOns));
        }
        
        booking.setTimeSlot(bookingData.get("timeSlot").toString());
        booking.setBookingDate(LocalDate.parse(bookingData.get("bookingDate").toString()));
        booking.setAddress(bookingData.get("address").toString());
        booking.setLandmark((String) bookingData.get("landmark"));
        booking.setSpecialInstructions((String) bookingData.get("specialInstructions"));
        booking.setTotalAmount(Double.valueOf(bookingData.get("totalAmount").toString()));
        
        if (bookingData.containsKey("estimatedDuration")) {
            booking.setEstimatedDuration(Double.valueOf(bookingData.get("estimatedDuration").toString()));
        }

        Booking savedBooking = bookingRepository.save(booking);
        
        // Notify all available technicians about new booking
        notificationService.notifyTechniciansNewBooking(savedBooking);
        
        return savedBooking;
    }

    /**
     * Technician accepts a pending booking
     */
    public Booking acceptBooking(UUID bookingId, UUID technicianId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingException("Booking not found", "BOOKING_NOT_FOUND"));

        // Validate booking is still pending and no technician assigned
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BookingException("Booking is no longer pending", "BOOKING_NOT_PENDING");
        }
        
        if (booking.getTechnicianId() != null) {
            throw new BookingException("Booking already has a technician assigned", "BOOKING_ALREADY_ASSIGNED");
        }

        // Validate technician
        User technician = userRepository.findById(technicianId)
                .orElseThrow(() -> new BookingException("Technician not found", "TECHNICIAN_NOT_FOUND"));

        if (!"technician".equals(technician.getRole())) {
            throw new BookingException("User is not a technician", "INVALID_TECHNICIAN_ROLE");
        }
        if (!technician.isVerified()) {
            throw new BookingException("Technician account is not verified", "TECHNICIAN_NOT_VERIFIED");
        }

        // Check technician availability
        TechnicianSettings settings = technicianSettingsRepository.findById(technicianId).orElse(null);
        if (settings != null && Boolean.FALSE.equals(settings.getIsAvailable())) {
            throw new BookingException("Technician is currently unavailable", "TECHNICIAN_UNAVAILABLE");
        }

        // Check workload limit (1 active + 1 upcoming booking)
        List<BookingStatus> activeStatuses = Arrays.asList(BookingStatus.CONFIRMED, BookingStatus.IN_PROGRESS);
        long activeBookings = bookingRepository.countByTechnicianIdAndStatusIn(technicianId, activeStatuses);
        
        if (activeBookings >= 2) {
            throw new BookingException("Technician has reached maximum workload (1 active + 1 upcoming booking)", "WORKLOAD_LIMIT_EXCEEDED");
        }

        // Check for time slot conflicts
        List<BookingStatus> conflictStatuses = Arrays.asList(BookingStatus.CONFIRMED, BookingStatus.IN_PROGRESS);
        long conflicts = bookingRepository.countByTechnicianIdAndBookingDateAndTimeSlotAndStatusIn(
            technicianId, booking.getBookingDate(), booking.getTimeSlot(), conflictStatuses
        );
        
        if (conflicts > 0) {
            throw new BookingException("Technician already has a booking at this time slot", "TIME_SLOT_CONFLICT");
        }

        // Assign technician and confirm booking
        booking.setTechnicianId(technicianId);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setConfirmedAt(LocalDateTime.now());

        Booking savedBooking = bookingRepository.save(booking);
        
        // Send notifications
        notificationService.notifyStatusChange(savedBooking, BookingStatus.CONFIRMED, "Booking accepted by technician");
        
        return savedBooking;
    }

    /**
     * Get all pending bookings (for technician dashboard)
     */
    public List<Booking> getPendingBookings() {
        return bookingRepository.findByStatusAndTechnicianIdIsNullOrderByCreatedAtAsc(BookingStatus.PENDING);
    }

    /**
     * Get technician's accepted bookings
     */
    public List<Booking> getTechnicianBookings(UUID technicianId) {
        return bookingRepository.findByTechnicianIdOrderByCreatedAtDesc(technicianId);
    }

    /**
     * Get pending bookings for technician dashboard (with limited info)
     */
    public List<Map<String, Object>> getPendingBookingsForTechnicians() {
        List<Booking> bookings = getPendingBookings();
        List<Map<String, Object>> response = new ArrayList<>();
        
        for (Booking booking : bookings) {
            Map<String, Object> map = createBookingResponseMap(booking, false); // Don't show address until accepted
            response.add(map);
        }
        
        return response;
    }

    /**
     * Get client bookings
     */
    public List<Booking> getClientBookings(UUID clientId) {
        return bookingRepository.findByClientIdOrderByCreatedAtDesc(clientId);
    }

    /**
     * Get booking details with user information
     */
    public Map<String, Object> getBookingDetails(UUID bookingId, UUID requestingUserId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingException("Booking not found", "BOOKING_NOT_FOUND"));
        
        // Determine if address should be shown
        boolean showAddress = false;
        if (booking.getClientId().equals(requestingUserId)) {
            showAddress = true; // Client can always see their address
        } else if (booking.getTechnicianId() != null && booking.getTechnicianId().equals(requestingUserId) && 
                   booking.getStatus() != BookingStatus.PENDING) {
            showAddress = true; // Assigned technician can see address after confirmation
        }
        
        return createBookingResponseMap(booking, showAddress);
    }

    /**
     * Cancel booking
     */
    public Booking cancelBooking(UUID bookingId, UUID userId, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingException("Booking not found", "BOOKING_NOT_FOUND"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BookingException("User not found", "USER_NOT_FOUND"));

        // Validate cancellation permissions
        if (booking.getStatus() == BookingStatus.PENDING && booking.getClientId().equals(userId)) {
            // Client can cancel pending bookings - validation passed
        } else if ("admin".equals(user.getRole())) {
            // Admin can cancel any booking - validation passed
        } else {
            throw new BookingException("You don't have permission to cancel this booking", "INSUFFICIENT_PERMISSIONS");
        }

        BookingStatus oldStatus = booking.getStatus();
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setStatusReason(reason);
        booking.setCancelledAt(LocalDateTime.now());

        Booking savedBooking = bookingRepository.save(booking);
        
        // Send notifications
        notificationService.notifyStatusChange(savedBooking, BookingStatus.CANCELLED, reason);
        
        return savedBooking;
    }

    /**
     * Toggle technician availability
     */
    public TechnicianSettings toggleTechnicianAvailability(UUID technicianId, boolean isAvailable) {
        User technician = userRepository.findById(technicianId)
                .orElseThrow(() -> new BookingException("Technician not found", "TECHNICIAN_NOT_FOUND"));

        if (!"technician".equals(technician.getRole())) {
            throw new BookingException("User is not a technician", "INVALID_TECHNICIAN_ROLE");
        }

        TechnicianSettings settings = technicianSettingsRepository.findById(technicianId)
                .orElse(new TechnicianSettings(technicianId, true));
        
        settings.setIsAvailable(isAvailable);
        return technicianSettingsRepository.save(settings);
    }

    /**
     * Update booking status (for technicians to update progress)
     */
    public Booking updateBookingStatus(UUID bookingId, BookingStatus newStatus, UUID updatedBy, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingException("Booking not found", "BOOKING_NOT_FOUND"));

        BookingStatus oldStatus = booking.getStatus();

        // Validate status transition
        if (!booking.getStatus().canTransitionTo(newStatus)) {
            throw new BookingException(
                String.format("Cannot transition from %s to %s", 
                    booking.getStatus().getValue(), newStatus.getValue()),
                "INVALID_STATUS_TRANSITION"
            );
        }

        // Validate user permissions for status change
        User user = userRepository.findById(updatedBy)
                .orElseThrow(() -> new BookingException("User not found", "USER_NOT_FOUND"));
        
        validateStatusChangePermission(booking, newStatus, user);

        // Update status and related fields
        booking.setStatus(newStatus);
        booking.setStatusReason(reason);
        
        // Set timestamp based on status
        switch (newStatus) {
            case CONFIRMED -> booking.setConfirmedAt(LocalDateTime.now());
            case IN_PROGRESS -> booking.setStartedAt(LocalDateTime.now());
            case COMPLETED -> booking.setCompletedAt(LocalDateTime.now());
            case CANCELLED -> booking.setCancelledAt(LocalDateTime.now());
            case NO_SHOW -> booking.setNoShowAt(LocalDateTime.now());
        }

        Booking savedBooking = bookingRepository.save(booking);
        
        // Send notifications about status change
        notificationService.notifyStatusChange(savedBooking, newStatus, reason);
        
        return savedBooking;
    }

    /**
     * Validate if user has permission to change booking status
     */
    private void validateStatusChangePermission(Booking booking, BookingStatus newStatus, User user) {
        switch (newStatus) {
            case CONFIRMED -> {
                // Only technicians can confirm (by accepting the booking)
                if (!"technician".equals(user.getRole())) {
                    throw new BookingException("Only technicians can confirm bookings", "INSUFFICIENT_PERMISSIONS");
                }
            }
            case IN_PROGRESS, COMPLETED, NO_SHOW -> {
                // Only assigned technician can update these statuses
                if (!booking.getTechnicianId().equals(user.getId())) {
                    throw new BookingException("Only assigned technician can update this status", "INSUFFICIENT_PERMISSIONS");
                }
            }
            case CANCELLED -> {
                // Client can cancel pending bookings, admin can cancel any
                if (booking.getStatus() == BookingStatus.PENDING && booking.getClientId().equals(user.getId())) {
                    // Client can cancel pending bookings
                    return;
                }
                if (!"admin".equals(user.getRole())) {
                    throw new BookingException("Only admin can cancel confirmed bookings", "INSUFFICIENT_PERMISSIONS");
                }
            }
        }
    }

    /**
     * Get booking statistics for admin dashboard
     */
    public Map<String, Object> getBookingStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // Count bookings by status
        for (BookingStatus status : BookingStatus.values()) {
            long count = bookingRepository.countByStatus(status);
            stats.put(status.getValue(), count);
        }
        
        // Total bookings
        long totalBookings = bookingRepository.count();
        stats.put("total", totalBookings);
        
        // Today's bookings
        LocalDate today = LocalDate.now();
        long todayBookings = bookingRepository.countByBookingDate(today);
        stats.put("today", todayBookings);
        
        // This week's bookings
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDate weekEnd = weekStart.plusDays(6);
        long weekBookings = bookingRepository.countByBookingDateBetween(weekStart, weekEnd);
        stats.put("thisWeek", weekBookings);
        
        // This month's bookings
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
        long monthBookings = bookingRepository.countByBookingDateBetween(monthStart, monthEnd);
        stats.put("thisMonth", monthBookings);
        
        // Revenue statistics (completed bookings)
        Double totalRevenue = bookingRepository.sumTotalAmountByStatus(BookingStatus.COMPLETED);
        stats.put("totalRevenue", totalRevenue != null ? totalRevenue : 0.0);
        
        Double monthRevenue = bookingRepository.sumTotalAmountByStatusAndBookingDateBetween(
            BookingStatus.COMPLETED, monthStart, monthEnd);
        stats.put("monthRevenue", monthRevenue != null ? monthRevenue : 0.0);
        
        return stats;
    }

    /**
     * Get recent bookings for admin dashboard
     */
    public List<Map<String, Object>> getRecentBookings(int limit) {
        List<Booking> recentBookings;
        if (limit <= 10) {
            recentBookings = bookingRepository.findTop10ByOrderByCreatedAtDesc();
        } else {
            recentBookings = bookingRepository.findTop20ByOrderByCreatedAtDesc();
        }
        
        List<Map<String, Object>> response = new ArrayList<>();
        
        int count = Math.min(limit, recentBookings.size());
        for (int i = 0; i < count; i++) {
            Booking booking = recentBookings.get(i);
            Map<String, Object> map = new HashMap<>();
            map.put("id", booking.getId());
            map.put("bookingCode", booking.getBookingCode());
            map.put("serviceType", booking.getServiceType());
            map.put("status", booking.getStatus().getValue());
            map.put("totalAmount", booking.getTotalAmount());
            map.put("bookingDate", booking.getBookingDate());
            map.put("createdAt", booking.getCreatedAt());
            
            // Add client info
            User client = userRepository.findById(booking.getClientId()).orElse(null);
            if (client != null) {
                map.put("clientName", client.getName());
            }
            
            // Add technician info if assigned
            if (booking.getTechnicianId() != null) {
                User technician = userRepository.findById(booking.getTechnicianId()).orElse(null);
                if (technician != null) {
                    map.put("technicianName", technician.getName());
                }
            }
            
            response.add(map);
        }
        
        return response;
    }

    /**
     * Get technician availability status
     */
    public boolean getTechnicianAvailability(UUID technicianId) {
        TechnicianSettings settings = technicianSettingsRepository.findById(technicianId).orElse(null);
        return settings == null || Boolean.TRUE.equals(settings.getIsAvailable()); // Default to available if no settings
    }

    /**
     * Get all bookings for admin dashboard with pagination
     */
    public List<Map<String, Object>> getAllBookingsForAdmin(int page, int size) {
        List<Booking> bookings = bookingRepository.findAllByOrderByCreatedAtDesc();
        List<Map<String, Object>> response = new ArrayList<>();
        
        int start = page * size;
        int end = Math.min(start + size, bookings.size());
        
        for (int i = start; i < end; i++) {
            Booking booking = bookings.get(i);
            Map<String, Object> map = createBookingResponseMap(booking, true); // Admin sees all details
            response.add(map);
        }
        
        return response;
    }

    /**
     * Get bookings by status
     */
    public List<Booking> getBookingsByStatus(BookingStatus status) {
        return bookingRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    /**
     * Create response map with booking and user details
     */
    private Map<String, Object> createBookingResponseMap(Booking booking, boolean includeAddress) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", booking.getId());
        map.put("bookingCode", booking.getBookingCode());
        map.put("serviceType", booking.getServiceType());
        map.put("deviceType", booking.getDeviceType());
        map.put("addOns", booking.getAddOns() != null ? 
            Arrays.asList(booking.getAddOns().split(",")) : new ArrayList<>());
        map.put("timeSlot", booking.getTimeSlot());
        map.put("bookingDate", booking.getBookingDate());
        
        // Address privacy: only show if user has permission
        if (includeAddress) {
            map.put("address", booking.getAddress());
            map.put("landmark", booking.getLandmark());
        } else {
            map.put("address", "Address will be visible after booking confirmation");
            map.put("landmark", null);
        }
        
        map.put("specialInstructions", booking.getSpecialInstructions());
        map.put("totalAmount", booking.getTotalAmount());
        map.put("status", booking.getStatus().getValue());
        map.put("statusDescription", booking.getStatus().getDescription());
        map.put("statusReason", booking.getStatusReason());
        map.put("paymentStatus", booking.getPaymentStatus());
        map.put("estimatedDuration", booking.getEstimatedDuration());
        map.put("createdAt", booking.getCreatedAt());
        map.put("updatedAt", booking.getUpdatedAt());
        map.put("confirmedAt", booking.getConfirmedAt());
        map.put("startedAt", booking.getStartedAt());
        map.put("completedAt", booking.getCompletedAt());
        map.put("cancelledAt", booking.getCancelledAt());
        map.put("noShowAt", booking.getNoShowAt());
        
        // Add user details
        if (booking.getTechnicianId() != null) {
            User technician = userRepository.findById(booking.getTechnicianId()).orElse(null);
            if (technician != null) {
                map.put("technicianName", technician.getName());
                map.put("technicianContact", technician.getContactNo());
                map.put("technicianEmail", technician.getEmail());
            }
        }
        
        User client = userRepository.findById(booking.getClientId()).orElse(null);
        if (client != null) {
            map.put("clientName", client.getName());
            map.put("clientContact", client.getContactNo());
            map.put("clientEmail", client.getEmail());
        }
        
        return map;
    }
}