package com.G4.backend;

import com.G4.backend.entity.*;
import com.G4.backend.enums.BookingStatus;
import com.G4.backend.enums.PhotoType;
import com.G4.backend.exception.BookingException;
import com.G4.backend.repository.*;
import com.G4.backend.service.BookingNotificationService;
import com.G4.backend.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Preservation Property Tests for Pre-Service Checklist Workflow Fix
 * 
 * **Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6**
 * 
 * CRITICAL: These tests MUST PASS on UNFIXED code - they capture baseline behavior to preserve
 * 
 * This test suite verifies that non-checklist operations remain unchanged:
 * - Booking creation (client validation, add-on compatibility)
 * - Technician acceptance (PENDING → CONFIRMED with availability, workload, time slot checks)
 * - Photo validation at IN_PROGRESS → COMPLETED transition
 * - Status transitions to CANCELLED and NO_SHOW
 * - Checklist item toggling (technician assignment verification)
 * 
 * These tests should PASS both before and after the fix, ensuring no regressions.
 */
public class PreservationPropertyTest {

    private BookingService bookingService;
    private BookingRepository bookingRepository;
    private UserRepository userRepository;
    private TechnicianSettingsRepository technicianSettingsRepository;
    private BookingNotificationService notificationService;
    private ServiceRepository serviceRepository;
    private AddOnRepository addOnRepository;
    private ServiceAllowedAddonRepository serviceAllowedAddonRepository;
    private ChecklistItemRepository checklistItemRepository;
    private BookingAddonRepository bookingAddonRepository;
    private BookingChecklistRepository bookingChecklistRepository;
    private BookingPhotoRepository bookingPhotoRepository;

    private User mockClient;
    private User mockTechnician;
    private User mockAdmin;
    private Booking mockBooking;

    @BeforeEach
    void setUp() {
        // Mock repositories and services
        bookingRepository = mock(BookingRepository.class);
        userRepository = mock(UserRepository.class);
        technicianSettingsRepository = mock(TechnicianSettingsRepository.class);
        notificationService = mock(BookingNotificationService.class);
        serviceRepository = mock(ServiceRepository.class);
        addOnRepository = mock(AddOnRepository.class);
        serviceAllowedAddonRepository = mock(ServiceAllowedAddonRepository.class);
        checklistItemRepository = mock(ChecklistItemRepository.class);
        bookingAddonRepository = mock(BookingAddonRepository.class);
        bookingChecklistRepository = mock(BookingChecklistRepository.class);
        bookingPhotoRepository = mock(BookingPhotoRepository.class);

        bookingService = new BookingService(
            bookingRepository,
            userRepository,
            technicianSettingsRepository,
            notificationService,
            serviceRepository,
            addOnRepository,
            serviceAllowedAddonRepository,
            checklistItemRepository,
            bookingAddonRepository,
            bookingChecklistRepository,
            bookingPhotoRepository
        );

        // Create mock users
        mockClient = createMockUser("client", true);
        mockTechnician = createMockUser("technician", true);
        mockAdmin = createMockUser("admin", true);

        // Setup mock responses
        when(userRepository.findById(mockClient.getId())).thenReturn(Optional.of(mockClient));
        when(userRepository.findById(mockTechnician.getId())).thenReturn(Optional.of(mockTechnician));
        when(userRepository.findById(mockAdmin.getId())).thenReturn(Optional.of(mockAdmin));

        // Create mock booking
        mockBooking = createMockBooking();
        when(bookingRepository.save(any(Booking.class))).thenReturn(mockBooking);
        when(bookingRepository.findById(mockBooking.getId())).thenReturn(Optional.of(mockBooking));
    }

    /**
     * Property 1: Booking Creation Preservation
     * 
     * **Validates: Requirement 3.1**
     * 
     * For any valid booking creation request, the system SHALL:
     * - Validate client exists and is verified
     * - Validate client role is "client"
     * - Validate add-on compatibility if serviceId is provided
     * - Create booking with PENDING status
     * - Notify technicians about new booking
     * 
     * This behavior must remain unchanged regardless of checklist workflow changes.
     */
    @Test
    void testBookingCreationPreservesClientValidation() {
        System.out.println("\n=== Preservation Test 1: Booking Creation - Client Validation ===");
        
        // Test 1a: Valid client creates booking successfully
        Map<String, Object> validBookingData = createBookingData();
        Booking createdBooking = bookingService.createBooking(validBookingData);
        
        assertNotNull(createdBooking, "Booking should be created successfully");
        assertEquals(BookingStatus.PENDING, createdBooking.getStatus(), "New booking should have PENDING status");
        assertEquals(mockClient.getId(), createdBooking.getClientId(), "Booking should be associated with client");
        verify(notificationService, times(1)).notifyTechniciansNewBooking(any(Booking.class));
        
        System.out.println("✓ PASS: Valid client can create booking");
        
        // Test 1b: Unverified client cannot create booking
        User unverifiedClient = createMockUser("client", false);
        when(userRepository.findById(unverifiedClient.getId())).thenReturn(Optional.of(unverifiedClient));
        
        Map<String, Object> unverifiedBookingData = createBookingData();
        unverifiedBookingData.put("clientId", unverifiedClient.getId().toString());
        
        BookingException exception = assertThrows(BookingException.class, () -> {
            bookingService.createBooking(unverifiedBookingData);
        });
        
        assertTrue(exception.getMessage().contains("not verified"), 
            "Should reject unverified client");
        assertEquals("CLIENT_NOT_VERIFIED", exception.getErrorCode());
        
        System.out.println("✓ PASS: Unverified client cannot create booking");
        
        // Test 1c: Non-client role cannot create booking
        User technicianAsClient = createMockUser("technician", true);
        when(userRepository.findById(technicianAsClient.getId())).thenReturn(Optional.of(technicianAsClient));
        
        Map<String, Object> wrongRoleBookingData = createBookingData();
        wrongRoleBookingData.put("clientId", technicianAsClient.getId().toString());
        
        BookingException roleException = assertThrows(BookingException.class, () -> {
            bookingService.createBooking(wrongRoleBookingData);
        });
        
        assertTrue(roleException.getMessage().contains("not a client"), 
            "Should reject non-client role");
        assertEquals("INVALID_CLIENT_ROLE", roleException.getErrorCode());
        
        System.out.println("✓ PASS: Non-client role cannot create booking");
        System.out.println("✓ PRESERVATION VERIFIED: Booking creation validation unchanged");
    }

    /**
     * Property 2: Technician Acceptance Preservation
     * 
     * **Validates: Requirement 3.1**
     * 
     * For any technician accepting a booking (PENDING → CONFIRMED), the system SHALL:
     * - Validate technician exists and is verified
     * - Validate technician role is "technician"
     * - Check technician availability
     * - Check workload limit (max 2 active bookings)
     * - Check for time slot conflicts
     * - Assign technician and set status to CONFIRMED
     * 
     * This behavior must remain unchanged regardless of checklist workflow changes.
     */
    @Test
    void testTechnicianAcceptancePreservesValidation() {
        System.out.println("\n=== Preservation Test 2: Technician Acceptance - Validation ===");
        
        // Setup: Mock booking in PENDING status
        mockBooking.setStatus(BookingStatus.PENDING);
        mockBooking.setTechnicianId(null);
        
        // Mock technician settings (available)
        TechnicianSettings settings = new TechnicianSettings(mockTechnician.getId(), true);
        when(technicianSettingsRepository.findById(mockTechnician.getId())).thenReturn(Optional.of(settings));
        
        // Mock workload check (0 active bookings)
        when(bookingRepository.countByTechnicianIdAndStatusIn(eq(mockTechnician.getId()), any())).thenReturn(0L);
        
        // Mock time slot conflict check (no conflicts)
        when(bookingRepository.countByTechnicianIdAndBookingDateAndTimeSlotAndStatusIn(
            eq(mockTechnician.getId()), any(), any(), any())).thenReturn(0L);
        
        // Test 2a: Valid technician accepts booking successfully
        Booking acceptedBooking = bookingService.acceptBooking(mockBooking.getId(), mockTechnician.getId());
        
        assertNotNull(acceptedBooking, "Booking should be accepted");
        assertEquals(BookingStatus.CONFIRMED, acceptedBooking.getStatus(), "Status should be CONFIRMED");
        assertEquals(mockTechnician.getId(), acceptedBooking.getTechnicianId(), "Technician should be assigned");
        assertNotNull(acceptedBooking.getConfirmedAt(), "Confirmed timestamp should be set");
        
        System.out.println("✓ PASS: Valid technician can accept booking");
        
        // Test 2b: Unavailable technician cannot accept booking
        TechnicianSettings unavailableSettings = new TechnicianSettings(mockTechnician.getId(), false);
        when(technicianSettingsRepository.findById(mockTechnician.getId())).thenReturn(Optional.of(unavailableSettings));
        
        mockBooking.setStatus(BookingStatus.PENDING);
        mockBooking.setTechnicianId(null);
        
        BookingException unavailableException = assertThrows(BookingException.class, () -> {
            bookingService.acceptBooking(mockBooking.getId(), mockTechnician.getId());
        });
        
        assertTrue(unavailableException.getMessage().contains("unavailable"), 
            "Should reject unavailable technician");
        assertEquals("TECHNICIAN_UNAVAILABLE", unavailableException.getErrorCode());
        
        System.out.println("✓ PASS: Unavailable technician cannot accept booking");
        
        // Test 2c: Technician with full workload cannot accept booking
        when(technicianSettingsRepository.findById(mockTechnician.getId())).thenReturn(Optional.of(settings));
        when(bookingRepository.countByTechnicianIdAndStatusIn(eq(mockTechnician.getId()), any())).thenReturn(2L);
        
        mockBooking.setStatus(BookingStatus.PENDING);
        mockBooking.setTechnicianId(null);
        
        BookingException workloadException = assertThrows(BookingException.class, () -> {
            bookingService.acceptBooking(mockBooking.getId(), mockTechnician.getId());
        });
        
        assertTrue(workloadException.getMessage().contains("workload"), 
            "Should reject technician with full workload");
        assertEquals("WORKLOAD_LIMIT_EXCEEDED", workloadException.getErrorCode());
        
        System.out.println("✓ PASS: Technician with full workload cannot accept booking");
        
        // Test 2d: Technician with time slot conflict cannot accept booking
        when(bookingRepository.countByTechnicianIdAndStatusIn(eq(mockTechnician.getId()), any())).thenReturn(0L);
        when(bookingRepository.countByTechnicianIdAndBookingDateAndTimeSlotAndStatusIn(
            eq(mockTechnician.getId()), any(), any(), any())).thenReturn(1L);
        
        mockBooking.setStatus(BookingStatus.PENDING);
        mockBooking.setTechnicianId(null);
        
        BookingException conflictException = assertThrows(BookingException.class, () -> {
            bookingService.acceptBooking(mockBooking.getId(), mockTechnician.getId());
        });
        
        assertTrue(conflictException.getMessage().contains("time slot"), 
            "Should reject technician with time slot conflict");
        assertEquals("TIME_SLOT_CONFLICT", conflictException.getErrorCode());
        
        System.out.println("✓ PASS: Technician with time slot conflict cannot accept booking");
        System.out.println("✓ PRESERVATION VERIFIED: Technician acceptance validation unchanged");
    }

    /**
     * Property 3: Photo Validation Preservation
     * 
     * **Validates: Requirement 3.2**
     * 
     * For any booking transitioning from IN_PROGRESS to COMPLETED, the system SHALL:
     * - Validate that at least 1 before-service photo is uploaded
     * - Validate that at least 1 after-service photo is uploaded
     * - Throw BookingException if photo requirements are not met
     * 
     * This behavior must remain unchanged regardless of checklist workflow changes.
     */
    @Test
    void testPhotoValidationPreservedAtCompletion() {
        System.out.println("\n=== Preservation Test 3: Photo Validation at Completion ===");
        
        // Setup: Mock booking in IN_PROGRESS status
        mockBooking.setStatus(BookingStatus.IN_PROGRESS);
        mockBooking.setTechnicianId(mockTechnician.getId());
        
        // Mock checklist validation (all complete) - this should pass
        when(bookingChecklistRepository.countTotalByBookingId(any())).thenReturn(5L);
        when(bookingChecklistRepository.countCheckedByBookingId(any())).thenReturn(5L);
        
        // Test 3a: Completion fails without photos
        when(bookingPhotoRepository.countByBookingIdAndType(any(), eq(PhotoType.BEFORE))).thenReturn(0L);
        when(bookingPhotoRepository.countByBookingIdAndType(any(), eq(PhotoType.AFTER))).thenReturn(0L);
        
        BookingException noPhotosException = assertThrows(BookingException.class, () -> {
            bookingService.updateBookingStatus(
                mockBooking.getId(),
                BookingStatus.COMPLETED,
                mockTechnician.getId(),
                "Completing service"
            );
        });
        
        assertTrue(noPhotosException.getMessage().contains("Photo") || noPhotosException.getMessage().contains("photo"),
            "Exception should mention photo requirement");
        assertEquals("PHOTOS_MISSING", noPhotosException.getErrorCode());
        
        System.out.println("✓ PASS: Completion fails without photos");
        
        // Test 3b: Completion fails with only before photos
        when(bookingPhotoRepository.countByBookingIdAndType(any(), eq(PhotoType.BEFORE))).thenReturn(2L);
        when(bookingPhotoRepository.countByBookingIdAndType(any(), eq(PhotoType.AFTER))).thenReturn(0L);
        
        BookingException noAfterPhotosException = assertThrows(BookingException.class, () -> {
            bookingService.updateBookingStatus(
                mockBooking.getId(),
                BookingStatus.COMPLETED,
                mockTechnician.getId(),
                "Completing service"
            );
        });
        
        assertTrue(noAfterPhotosException.getMessage().contains("after"),
            "Exception should mention after-service photos");
        
        System.out.println("✓ PASS: Completion fails with only before photos");
        
        // Test 3c: Completion fails with only after photos
        when(bookingPhotoRepository.countByBookingIdAndType(any(), eq(PhotoType.BEFORE))).thenReturn(0L);
        when(bookingPhotoRepository.countByBookingIdAndType(any(), eq(PhotoType.AFTER))).thenReturn(2L);
        
        BookingException noBeforePhotosException = assertThrows(BookingException.class, () -> {
            bookingService.updateBookingStatus(
                mockBooking.getId(),
                BookingStatus.COMPLETED,
                mockTechnician.getId(),
                "Completing service"
            );
        });
        
        assertTrue(noBeforePhotosException.getMessage().contains("before"),
            "Exception should mention before-service photos");
        
        System.out.println("✓ PASS: Completion fails with only after photos");
        
        // Test 3d: Completion succeeds with both photo types
        when(bookingPhotoRepository.countByBookingIdAndType(any(), eq(PhotoType.BEFORE))).thenReturn(2L);
        when(bookingPhotoRepository.countByBookingIdAndType(any(), eq(PhotoType.AFTER))).thenReturn(2L);
        
        Booking completedBooking = bookingService.updateBookingStatus(
            mockBooking.getId(),
            BookingStatus.COMPLETED,
            mockTechnician.getId(),
            "Service completed"
        );
        
        assertEquals(BookingStatus.COMPLETED, completedBooking.getStatus(), "Status should be COMPLETED");
        assertNotNull(completedBooking.getCompletedAt(), "Completed timestamp should be set");
        
        System.out.println("✓ PASS: Completion succeeds with both photo types");
        System.out.println("✓ PRESERVATION VERIFIED: Photo validation unchanged");
    }

    /**
     * Property 4: Status Transition to CANCELLED Preservation
     * 
     * **Validates: Requirement 3.6**
     * 
     * For any booking transitioning to CANCELLED status, the system SHALL:
     * - Allow client to cancel PENDING bookings
     * - Allow admin to cancel any booking
     * - Reject cancellation by unauthorized users
     * - NOT validate checklist completion
     * 
     * This behavior must remain unchanged regardless of checklist workflow changes.
     */
    @Test
    void testCancellationPreservesPermissionChecks() {
        System.out.println("\n=== Preservation Test 4: Cancellation Permission Checks ===");
        
        // Test 4a: Client can cancel PENDING booking
        mockBooking.setStatus(BookingStatus.PENDING);
        mockBooking.setClientId(mockClient.getId());
        
        Booking cancelledByClient = bookingService.cancelBooking(
            mockBooking.getId(),
            mockClient.getId(),
            "Client changed mind"
        );
        
        assertEquals(BookingStatus.CANCELLED, cancelledByClient.getStatus(), "Status should be CANCELLED");
        assertEquals("Client changed mind", cancelledByClient.getStatusReason(), "Reason should be recorded");
        assertNotNull(cancelledByClient.getCancelledAt(), "Cancelled timestamp should be set");
        
        System.out.println("✓ PASS: Client can cancel PENDING booking");
        
        // Test 4b: Admin can cancel any booking
        mockBooking.setStatus(BookingStatus.CONFIRMED);
        
        Booking cancelledByAdmin = bookingService.cancelBooking(
            mockBooking.getId(),
            mockAdmin.getId(),
            "Admin cancellation"
        );
        
        assertEquals(BookingStatus.CANCELLED, cancelledByAdmin.getStatus(), "Status should be CANCELLED");
        
        System.out.println("✓ PASS: Admin can cancel any booking");
        
        // Test 4c: Non-admin cannot cancel CONFIRMED booking
        mockBooking.setStatus(BookingStatus.CONFIRMED);
        
        BookingException unauthorizedException = assertThrows(BookingException.class, () -> {
            bookingService.cancelBooking(
                mockBooking.getId(),
                mockClient.getId(),
                "Unauthorized cancellation"
            );
        });
        
        assertTrue(unauthorizedException.getMessage().contains("permission"),
            "Should reject unauthorized cancellation");
        
        System.out.println("✓ PASS: Non-admin cannot cancel CONFIRMED booking");
        System.out.println("✓ PRESERVATION VERIFIED: Cancellation permission checks unchanged");
    }

    /**
     * Property 5: Status Transition to NO_SHOW Preservation
     * 
     * **Validates: Requirement 3.6**
     * 
     * For any booking transitioning to NO_SHOW status, the system SHALL:
     * - Allow assigned technician to mark as NO_SHOW
     * - Reject NO_SHOW by non-assigned users
     * - NOT validate checklist completion
     * 
     * This behavior must remain unchanged regardless of checklist workflow changes.
     */
    @Test
    void testNoShowPreservesPermissionChecks() {
        System.out.println("\n=== Preservation Test 5: NO_SHOW Permission Checks ===");
        
        // Setup: Mock booking in CONFIRMED status with assigned technician
        mockBooking.setStatus(BookingStatus.CONFIRMED);
        mockBooking.setTechnicianId(mockTechnician.getId());
        
        // Test 5a: Assigned technician can mark as NO_SHOW
        Booking noShowBooking = bookingService.updateBookingStatus(
            mockBooking.getId(),
            BookingStatus.NO_SHOW,
            mockTechnician.getId(),
            "Client not available"
        );
        
        assertEquals(BookingStatus.NO_SHOW, noShowBooking.getStatus(), "Status should be NO_SHOW");
        assertEquals("Client not available", noShowBooking.getStatusReason(), "Reason should be recorded");
        assertNotNull(noShowBooking.getNoShowAt(), "NO_SHOW timestamp should be set");
        
        System.out.println("✓ PASS: Assigned technician can mark as NO_SHOW");
        
        // Test 5b: Non-assigned technician cannot mark as NO_SHOW
        User otherTechnician = createMockUser("technician", true);
        when(userRepository.findById(otherTechnician.getId())).thenReturn(Optional.of(otherTechnician));
        
        mockBooking.setStatus(BookingStatus.CONFIRMED);
        
        BookingException unauthorizedException = assertThrows(BookingException.class, () -> {
            bookingService.updateBookingStatus(
                mockBooking.getId(),
                BookingStatus.NO_SHOW,
                otherTechnician.getId(),
                "Unauthorized NO_SHOW"
            );
        });
        
        assertTrue(unauthorizedException.getMessage().contains("assigned technician"),
            "Should reject non-assigned technician");
        
        System.out.println("✓ PASS: Non-assigned technician cannot mark as NO_SHOW");
        System.out.println("✓ PRESERVATION VERIFIED: NO_SHOW permission checks unchanged");
    }

    /**
     * Property 6: Checklist Item Toggling Preservation
     * 
     * **Validates: Requirement 3.3**
     * 
     * For any checklist item toggle operation, the system SHALL:
     * - Verify that the technician is assigned to the booking
     * - Reject toggle by non-assigned technicians
     * - Toggle the item's checked status
     * 
     * This behavior must remain unchanged regardless of checklist workflow changes.
     */
    @Test
    void testChecklistTogglePreservesTechnicianVerification() {
        System.out.println("\n=== Preservation Test 6: Checklist Toggle - Technician Verification ===");
        
        // Setup: Mock booking with assigned technician
        mockBooking.setStatus(BookingStatus.IN_PROGRESS);
        mockBooking.setTechnicianId(mockTechnician.getId());
        
        // Create mock checklist item
        ChecklistItem mockItem = new ChecklistItem("Test item", true);
        mockItem.setId(UUID.randomUUID());
        
        BookingChecklist mockBookingChecklist = new BookingChecklist(mockBooking, mockItem);
        mockBookingChecklist.setIsChecked(false);
        
        BookingChecklist.BookingChecklistId bcId = new BookingChecklist.BookingChecklistId(
            mockBooking.getId(), mockItem.getId()
        );
        
        when(bookingChecklistRepository.findById(bcId)).thenReturn(Optional.of(mockBookingChecklist));
        when(bookingChecklistRepository.save(any(BookingChecklist.class))).thenReturn(mockBookingChecklist);
        
        // Test 6a: Assigned technician can toggle checklist item
        BookingChecklist toggledItem = bookingService.toggleChecklistItem(
            mockBooking.getId(),
            mockItem.getId(),
            mockTechnician.getId()
        );
        
        assertNotNull(toggledItem, "Checklist item should be toggled");
        assertTrue(toggledItem.getIsChecked(), "Item should be checked after toggle");
        
        System.out.println("✓ PASS: Assigned technician can toggle checklist item");
        
        // Test 6b: Non-assigned technician cannot toggle checklist item
        User otherTechnician = createMockUser("technician", true);
        when(userRepository.findById(otherTechnician.getId())).thenReturn(Optional.of(otherTechnician));
        
        BookingException unauthorizedException = assertThrows(BookingException.class, () -> {
            bookingService.toggleChecklistItem(
                mockBooking.getId(),
                mockItem.getId(),
                otherTechnician.getId()
            );
        });
        
        assertTrue(unauthorizedException.getMessage().contains("assigned technician"),
            "Should reject non-assigned technician");
        assertEquals("INSUFFICIENT_PERMISSIONS", unauthorizedException.getErrorCode());
        
        System.out.println("✓ PASS: Non-assigned technician cannot toggle checklist item");
        System.out.println("✓ PRESERVATION VERIFIED: Checklist toggle technician verification unchanged");
    }

    // Helper methods

    private User createMockUser(String role, boolean verified) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName("Test " + role);
        user.setEmail("test" + role + "@example.com");
        user.setRole(role);
        user.setVerified(verified);
        user.setContactNo("1234567890");
        return user;
    }

    private Booking createMockBooking() {
        Booking booking = new Booking();
        booking.setId(UUID.randomUUID());
        booking.setClientId(mockClient.getId());
        booking.setServiceType("Deep Cleaning");
        booking.setDeviceType("Laptop");
        booking.setTimeSlot("10:00 AM - 12:00 PM");
        booking.setBookingDate(LocalDate.now().plusDays(3));
        booking.setAddress("123 Test Street");
        booking.setTotalAmount(150.0);
        booking.setStatus(BookingStatus.PENDING);
        booking.setPaymentStatus("pending");
        return booking;
    }

    private Map<String, Object> createBookingData() {
        Map<String, Object> data = new HashMap<>();
        data.put("clientId", mockClient.getId().toString());
        data.put("serviceType", "Deep Cleaning");
        data.put("deviceType", "Laptop");
        data.put("timeSlot", "10:00 AM - 12:00 PM");
        data.put("bookingDate", LocalDate.now().plusDays(3).toString());
        data.put("address", "123 Test Street");
        data.put("totalAmount", "150.0");
        data.put("estimatedDuration", "2.0");
        return data;
    }
}
