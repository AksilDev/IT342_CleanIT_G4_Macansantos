package com.G4.backend.service;

import com.G4.backend.entity.*;
import com.G4.backend.enums.BookingStatus;
import com.G4.backend.enums.PhotoType;
import com.G4.backend.exception.BookingException;
import com.G4.backend.repository.*;
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
    private final ServiceRepository serviceRepository;
    private final AddOnRepository addOnRepository;
    private final ServiceAllowedAddonRepository serviceAllowedAddonRepository;
    private final ChecklistItemRepository checklistItemRepository;
    private final BookingAddonRepository bookingAddonRepository;
    private final BookingChecklistRepository bookingChecklistRepository;
    private final BookingPhotoRepository bookingPhotoRepository;

    public BookingService(BookingRepository bookingRepository, UserRepository userRepository, 
                         TechnicianSettingsRepository technicianSettingsRepository,
                         BookingNotificationService notificationService,
                         ServiceRepository serviceRepository,
                         AddOnRepository addOnRepository,
                         ServiceAllowedAddonRepository serviceAllowedAddonRepository,
                         ChecklistItemRepository checklistItemRepository,
                         BookingAddonRepository bookingAddonRepository,
                         BookingChecklistRepository bookingChecklistRepository,
                         BookingPhotoRepository bookingPhotoRepository) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.technicianSettingsRepository = technicianSettingsRepository;
        this.notificationService = notificationService;
        this.serviceRepository = serviceRepository;
        this.addOnRepository = addOnRepository;
        this.serviceAllowedAddonRepository = serviceAllowedAddonRepository;
        this.checklistItemRepository = checklistItemRepository;
        this.bookingAddonRepository = bookingAddonRepository;
        this.bookingChecklistRepository = bookingChecklistRepository;
        this.bookingPhotoRepository = bookingPhotoRepository;
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
        
        boolean isPreAssigned = false;
        
        // Check if client pre-assigned a technician
        if (bookingData.containsKey("technicianId") && bookingData.get("technicianId") != null) {
            UUID technicianId = UUID.fromString(bookingData.get("technicianId").toString());
            // Validate technician exists
            User technician = userRepository.findById(technicianId)
                    .orElseThrow(() -> new BookingException("Technician not found", "TECHNICIAN_NOT_FOUND"));
            if (!"technician".equals(technician.getRole())) {
                throw new BookingException("User is not a technician", "INVALID_TECHNICIAN_ROLE");
            }
            if (!technician.isVerified()) {
                throw new BookingException("Technician account is not verified", "TECHNICIAN_NOT_VERIFIED");
            }
            booking.setTechnicianId(technicianId);
            isPreAssigned = true;
            
            // Pre-assigned bookings are automatically confirmed
            booking.setStatus(BookingStatus.CONFIRMED);
            booking.setConfirmedAt(LocalDateTime.now());
        }
        // No technician assigned initially - they will accept the booking (status remains PENDING)
        
        booking.setServiceType(bookingData.get("serviceType").toString());
        
        // AC-10: Validate add-on compatibility if serviceId is provided
        if (bookingData.containsKey("serviceId") && bookingData.get("serviceId") != null) {
            UUID serviceId = UUID.fromString(bookingData.get("serviceId").toString());
            booking.setServiceId(serviceId);
            
            @SuppressWarnings("unchecked")
            List<String> addOnIds = (List<String>) bookingData.get("addOns");
            if (addOnIds != null && !addOnIds.isEmpty()) {
                // Convert String IDs to UUIDs
                List<UUID> addonUUIDs = addOnIds.stream()
                    .map(UUID::fromString)
                    .toList();
                
                // Validate compatibility
                String compatibilityError = validateAddonCompatibility(serviceId, addonUUIDs);
                if (compatibilityError != null) {
                    throw new BookingException(compatibilityError, "ADDON_INCOMPATIBLE");
                }
            }
        }
        
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
        
        // Checklist will be initialized when service starts (CONFIRMED -> IN_PROGRESS)
        // NOT initialized at booking creation to ensure proper workflow timing
        
        // Notify based on assignment status
        if (isPreAssigned) {
            // Pre-assigned booking: notify only the assigned technician and client
            notificationService.notifyStatusChange(savedBooking, BookingStatus.CONFIRMED, "Booking confirmed with pre-assigned technician");
        } else {
            // Unassigned booking: notify all available technicians about new booking
            notificationService.notifyTechniciansNewBooking(savedBooking);
        }
        
        return savedBooking;
    }

    /**
     * Technician accepts a pending booking
     */
    public Booking acceptBooking(UUID bookingId, UUID technicianId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingException("Booking not found", "BOOKING_NOT_FOUND"));

        // Validate booking is still pending
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BookingException("Booking is no longer pending", "BOOKING_NOT_PENDING");
        }
        
        // Check if booking is pre-assigned to this technician
        if (booking.getTechnicianId() != null) {
            // If pre-assigned to the requesting technician, auto-confirm it
            if (booking.getTechnicianId().equals(technicianId)) {
                booking.setStatus(BookingStatus.CONFIRMED);
                booking.setConfirmedAt(LocalDateTime.now());
                Booking savedBooking = bookingRepository.save(booking);
                notificationService.notifyStatusChange(savedBooking, BookingStatus.CONFIRMED, "Booking confirmed by assigned technician");
                return savedBooking;
            } else {
                // Pre-assigned to a different technician
                throw new BookingException("Booking is assigned to another technician", "BOOKING_ASSIGNED_TO_OTHER");
            }
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
     * 
     * Returns ONLY bookings specifically assigned to this technician with PENDING status.
     * Unassigned bookings (technicianId = NULL) are NOT visible to technicians.
     */
    public List<Map<String, Object>> getPendingBookingsForTechnicians(UUID technicianId) {
        // Validate technician exists and has correct role
        User technician = userRepository.findById(technicianId)
            .orElseThrow(() -> new BookingException("Technician not found", "TECHNICIAN_NOT_FOUND"));
        
        if (!"technician".equals(technician.getRole())) {
            throw new BookingException("User is not a technician", "INVALID_TECHNICIAN_ROLE");
        }
        
        // Get ONLY pending bookings assigned to this specific technician
        List<Booking> bookings = bookingRepository.findByTechnicianIdAndStatusOrderByCreatedAtAsc(
            technicianId, 
            BookingStatus.PENDING
        );
        
        System.out.println("DEBUG: Found " + bookings.size() + " pending bookings assigned to technician " + technicianId);
        
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

        // AC-11 FIX: Initialize checklist when starting service (CONFIRMED -> IN_PROGRESS)
        // Checklist should only be created when technician presses "Start Service"
        if (oldStatus == BookingStatus.CONFIRMED && newStatus == BookingStatus.IN_PROGRESS) {
            initializeBookingChecklist(booking);
        }

        // AC-11 FIX: Validate checklist completion before completing service (IN_PROGRESS -> COMPLETED)
        // Technician must complete all pre-service checklist items before marking service as completed
        if (oldStatus == BookingStatus.IN_PROGRESS && newStatus == BookingStatus.COMPLETED) {
            Map<String, Object> checklistValidation = validateChecklistComplete(bookingId);
            if (!(Boolean) checklistValidation.get("isComplete")) {
                @SuppressWarnings("unchecked")
                List<String> incompleteItems = (List<String>) checklistValidation.get("incompleteItems");
                throw new BookingException(
                    "All pre-service checklist items must be completed before finishing service. Missing: " + 
                    String.join(", ", incompleteItems),
                    "CHECKLIST_INCOMPLETE"
                );
            }
        }

        // AC-12: Validate photo uploads before In Progress -> Completed
        if (oldStatus == BookingStatus.IN_PROGRESS && newStatus == BookingStatus.COMPLETED) {
            Map<String, Object> photoValidation = validatePhotosUploaded(bookingId);
            if (!(Boolean) photoValidation.get("hasRequiredPhotos")) {
                @SuppressWarnings("unchecked")
                List<String> missingRequirements = (List<String>) photoValidation.get("missingRequirements");
                throw new BookingException(
                    "Photo documentation required: " + String.join(". ", missingRequirements),
                    "PHOTOS_MISSING"
                );
            }
        }

        // Update status and related fields
        booking.setStatus(newStatus);
        booking.setStatusReason(reason);
        
        // Set timestamp based on status
        switch (newStatus) {
            case CONFIRMED -> booking.setConfirmedAt(LocalDateTime.now());
            case IN_PROGRESS -> booking.setStartedAt(LocalDateTime.now());
            case COMPLETED -> booking.setCompletedAt(LocalDateTime.now());
            case CANCELLED -> booking.setCancelledAt(LocalDateTime.now());
            case NO_SHOW -> {
                booking.setNoShowAt(LocalDateTime.now());
                
                // BUG FIX 3: Auto-cancel booking when marked as NO_SHOW
                // Immediately transition to CANCELLED status
                booking.setStatus(BookingStatus.CANCELLED);
                booking.setCancelledAt(LocalDateTime.now());
                booking.setStatusReason("Cancelled due to no-show");
                
                System.out.println("DEBUG: NO_SHOW auto-cancelled - bookingId: " + bookingId + 
                                 ", noShowAt: " + booking.getNoShowAt() + 
                                 ", cancelledAt: " + booking.getCancelledAt());
            }
        }

        Booking savedBooking = bookingRepository.save(booking);
        
        // Send notifications about status change
        // For NO_SHOW, notify as CANCELLED (since we auto-transitioned)
        BookingStatus notificationStatus = (newStatus == BookingStatus.NO_SHOW) ? BookingStatus.CANCELLED : newStatus;
        String notificationReason = (newStatus == BookingStatus.NO_SHOW) ? "Cancelled due to no-show" : reason;
        notificationService.notifyStatusChange(savedBooking, notificationStatus, notificationReason);
        
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
        
        // Convert add-on UUIDs to names for display
        List<String> addOnNames = new ArrayList<>();
        if (booking.getAddOns() != null && !booking.getAddOns().isEmpty()) {
            String[] addOnIds = booking.getAddOns().split(",");
            for (String addOnId : addOnIds) {
                try {
                    UUID addOnUUID = UUID.fromString(addOnId.trim());
                    AddOn addOn = addOnRepository.findById(addOnUUID).orElse(null);
                    if (addOn != null) {
                        addOnNames.add(addOn.getName());
                    } else {
                        addOnNames.add(addOnId.trim()); // Fallback to UUID if not found
                    }
                } catch (IllegalArgumentException e) {
                    addOnNames.add(addOnId.trim()); // Keep as-is if not a valid UUID
                }
            }
        }
        map.put("addOns", addOnNames);
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
        map.put("technicianId", booking.getTechnicianId() != null ? booking.getTechnicianId().toString() : null); // Convert UUID to String
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

    /**
     * Reschedule booking
     */
    public Booking rescheduleBooking(UUID bookingId, LocalDate newDate, String newTimeSlot, UUID requestedBy, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingException("Booking not found", "BOOKING_NOT_FOUND"));

        // Only pending or confirmed bookings can be rescheduled
        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BookingException("Only pending or confirmed bookings can be rescheduled", "CANNOT_RESCHEDULE");
        }

        // Only client who created the booking can reschedule
        if (!booking.getClientId().equals(requestedBy)) {
            throw new BookingException("You can only reschedule your own bookings", "INSUFFICIENT_PERMISSIONS");
        }

        // Check if new date is in the future
        if (newDate.isBefore(LocalDate.now())) {
            throw new BookingException("New booking date must be in the future", "INVALID_DATE");
        }

        // If booking is confirmed, check technician availability for new slot
        if (booking.getStatus() == BookingStatus.CONFIRMED && booking.getTechnicianId() != null) {
            List<BookingStatus> conflictStatuses = Arrays.asList(BookingStatus.CONFIRMED, BookingStatus.IN_PROGRESS);
            long conflicts = bookingRepository.countByTechnicianIdAndBookingDateAndTimeSlotAndStatusIn(
                booking.getTechnicianId(), newDate, newTimeSlot, conflictStatuses
            );

            if (conflicts > 0) {
                throw new BookingException("Technician is not available at the requested time slot", "TIME_SLOT_CONFLICT");
            }
        }

        // Update booking
        booking.setBookingDate(newDate);
        booking.setTimeSlot(newTimeSlot);
        booking.setRescheduledAt(LocalDateTime.now());
        booking.setRescheduleReason(reason);
        booking.setUpdatedAt(LocalDateTime.now());

        Booking savedBooking = bookingRepository.save(booking);

        // Send notifications
        notificationService.notifyStatusChange(savedBooking, booking.getStatus(),
            "Booking rescheduled to " + newDate + " at " + newTimeSlot + ". Reason: " + reason);

        return savedBooking;
    }

    /**
     * Validate add-on compatibility based on selected service (AC-10)
     * Returns error message if incompatible, null if valid
     */
    public String validateAddonCompatibility(UUID serviceId, List<UUID> addonIds) {
        if (addonIds == null || addonIds.isEmpty()) {
            return null; // No add-ons selected, always valid
        }

        com.G4.backend.entity.Service service = serviceRepository.findById(serviceId)
            .orElseThrow(() -> new BookingException("Service not found", "SERVICE_NOT_FOUND"));

        // Check each add-on for compatibility
        for (UUID addonId : addonIds) {
            boolean isAllowed = serviceAllowedAddonRepository.existsByServiceIdAndAddonId(serviceId, addonId);
            
            if (!isAllowed) {
                AddOn addOn = addOnRepository.findById(addonId)
                    .orElseThrow(() -> new BookingException("Add-on not found", "ADDON_NOT_FOUND"));
                
                // Provide specific error message based on incompatibility
                if (service.getName().contains("Deep Internal Cleaning")) {
                    return "Deep Internal Cleaning cannot be combined with " + addOn.getName() + ". Please remove this add-on.";
                } else if (service.getName().contains("GPU")) {
                    return "GPU Deep Cleaning cannot be combined with " + addOn.getName() + ". Please remove this add-on.";
                } else {
                    return addOn.getName() + " is not compatible with " + service.getName();
                }
            }
        }

        return null; // All add-ons are compatible
    }

    /**
     * Validate that all checklist items are completed (AC-11)
     * Returns validation result with details
     */
    public Map<String, Object> validateChecklistComplete(UUID bookingId) {
        Map<String, Object> result = new HashMap<>();
        
        long totalItems = bookingChecklistRepository.countTotalByBookingId(bookingId);
        long checkedItems = bookingChecklistRepository.countCheckedByBookingId(bookingId);
        
        boolean isComplete = (totalItems > 0 && totalItems == checkedItems);
        
        result.put("isComplete", isComplete);
        result.put("totalItems", totalItems);
        result.put("checkedItems", checkedItems);
        result.put("percentage", totalItems > 0 ? (checkedItems * 100 / totalItems) : 0);
        
        if (!isComplete) {
            // Get incomplete items
            List<BookingChecklist> allChecklist = bookingChecklistRepository.findByIdBookingId(bookingId);
            List<String> incompleteItems = allChecklist.stream()
                .filter(bc -> !bc.getIsChecked())
                .map(bc -> bc.getChecklistItem().getLabel())
                .toList();
            
            result.put("incompleteItems", incompleteItems);
        }
        
        return result;
    }

    /**
     * Validate that required photos are uploaded (AC-12)
     * Returns validation result with details
     */
    public Map<String, Object> validatePhotosUploaded(UUID bookingId) {
        Map<String, Object> result = new HashMap<>();
        
        long beforePhotos = bookingPhotoRepository.countByBookingIdAndType(bookingId, PhotoType.BEFORE);
        long afterPhotos = bookingPhotoRepository.countByBookingIdAndType(bookingId, PhotoType.AFTER);
        
        boolean hasRequiredPhotos = (beforePhotos >= 1 && afterPhotos >= 1);
        
        result.put("hasRequiredPhotos", hasRequiredPhotos);
        result.put("beforePhotosCount", beforePhotos);
        result.put("afterPhotosCount", afterPhotos);
        result.put("meetsRequirement", hasRequiredPhotos);
        
        if (!hasRequiredPhotos) {
            List<String> missingRequirements = new ArrayList<>();
            if (beforePhotos == 0) {
                missingRequirements.add("At least 1 before-service photo is required");
            }
            if (afterPhotos == 0) {
                missingRequirements.add("At least 1 after-service photo is required");
            }
            result.put("missingRequirements", missingRequirements);
        }
        
        return result;
    }

    /**
     * Initialize checklist for a new booking
     */
    private void initializeBookingChecklist(Booking booking) {
        List<ChecklistItem> allItems = checklistItemRepository.findByIsActiveTrue();
        
        for (ChecklistItem item : allItems) {
            BookingChecklist bookingChecklist = new BookingChecklist(booking, item);
            bookingChecklistRepository.save(bookingChecklist);
        }
    }

    /**
     * Toggle checklist item status
     */
    public BookingChecklist toggleChecklistItem(UUID bookingId, UUID checklistItemId, UUID technicianId) {
        // Validate technician is assigned to this booking
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new BookingException("Booking not found", "BOOKING_NOT_FOUND"));
        
        if (!booking.getTechnicianId().equals(technicianId)) {
            throw new BookingException("Only assigned technician can update checklist", "INSUFFICIENT_PERMISSIONS");
        }
        
        // Find the booking checklist item
        BookingChecklist.BookingChecklistId bcId = new BookingChecklist.BookingChecklistId(bookingId, checklistItemId);
        BookingChecklist bookingChecklist = bookingChecklistRepository.findById(bcId)
            .orElseThrow(() -> new BookingException("Checklist item not found", "CHECKLIST_ITEM_NOT_FOUND"));
        
        // Toggle status
        bookingChecklist.setIsChecked(!bookingChecklist.getIsChecked());
        
        return bookingChecklistRepository.save(bookingChecklist);
    }

    /**
     * Get booking checklist with completion status
     */
    public List<Map<String, Object>> getBookingChecklist(UUID bookingId) {
        List<BookingChecklist> checklist = bookingChecklistRepository.findByIdBookingId(bookingId);
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (BookingChecklist bc : checklist) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", bc.getChecklistItem().getId());
            item.put("label", bc.getChecklistItem().getLabel());
            item.put("isChecked", bc.getIsChecked());
            item.put("checkedAt", bc.getCheckedAt());
            result.add(item);
        }
        
        return result;
    }

    /**
     * Get booking photos
     */
    public List<Map<String, Object>> getBookingPhotos(UUID bookingId) {
        List<BookingPhoto> photos = bookingPhotoRepository.findByBookingId(bookingId);
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (BookingPhoto photo : photos) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", photo.getId());
            item.put("type", photo.getType().toString());
            item.put("fileUrl", photo.getFileUrl());
            item.put("uploadedAt", photo.getUploadedAt());
            result.add(item);
        }
        
        return result;
    }

    /**
     * Add photo to booking
     */
    public BookingPhoto addBookingPhoto(UUID bookingId, String type, String fileUrl, UUID technicianId) {
        // Validate booking exists
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new BookingException("Booking not found", "BOOKING_NOT_FOUND"));
        
        // Validate technician is assigned
        if (!booking.getTechnicianId().equals(technicianId)) {
            throw new BookingException("Only assigned technician can upload photos", "INSUFFICIENT_PERMISSIONS");
        }
        
        // Parse photo type
        PhotoType photoType;
        try {
            photoType = PhotoType.valueOf(type);
            System.out.println("DEBUG: Photo type parsed: " + photoType + " (from: " + type + ")");
        } catch (IllegalArgumentException e) {
            throw new BookingException("Invalid photo type. Must be BEFORE or AFTER", "INVALID_PHOTO_TYPE");
        }
        
        // Create and save photo record
        BookingPhoto photo = new BookingPhoto(bookingId, photoType, fileUrl, technicianId);
        System.out.println("DEBUG: BookingPhoto created - bookingId: " + bookingId + ", type: " + photo.getType() + ", fileUrl: " + fileUrl + ", uploadedBy: " + technicianId);
        
        BookingPhoto savedPhoto = bookingPhotoRepository.save(photo);
        System.out.println("DEBUG: BookingPhoto saved with ID: " + savedPhoto.getId());
        
        return savedPhoto;
    }
}