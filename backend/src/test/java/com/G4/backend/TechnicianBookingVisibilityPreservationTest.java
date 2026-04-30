package com.G4.backend;

import com.G4.backend.entity.*;
import com.G4.backend.enums.BookingStatus;
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
 * Preservation Property Tests for Technician Booking Visibility Fix
 * 
 * **Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7**
 * 
 * CRITICAL: These tests MUST PASS on UNFIXED code - they capture baseline behavior to preserve
 * 
 * This test suite verifies that non-pending-bookings endpoints remain unchanged:
 * - Technician Full Bookings List (/api/v1/technician/{technicianId}/bookings)
 * - Booking Acceptance Flow (/api/v1/technician/bookings/{bookingId}/accept)
 * - Admin Booking View (if admin endpoints exist)
 * - Client Booking View
 * 
 * These tests should PASS both before and after the fix, ensuring no regressions.
 */
public class TechnicianBookingVisibilityPreservationTest {

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

    private User technicianA;
    private User mockClient;
    private User mockAdmin;

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
        technicianA = createMockTechnician("Tech A", "techA@example.com");
        mockClient = createMockUser("client", "client@example.com", true);
        mockAdmin = createMockUser("admin", "admin@example.com", true);

        // Setup mock responses for user lookups
        when(userRepository.findById(technicianA.getId())).thenReturn(Optional.of(technicianA));
        when(userRepository.findById(mockClient.getId())).thenReturn(Optional.of(mockClient));
        when(userRepository.findById(mockAdmin.getId())).thenReturn(Optional.of(mockAdmin));
    }

    /**
     * Property 1: Technician Full Bookings List Preservation
     * 
     * **Validates: Requirement 3.1**
     * 
     * Test: /api/v1/technician/{technicianId}/bookings
     * 
     * For any technician requesting their full bookings list, the system SHALL:
     * - Return ALL bookings assigned to that technician
     * - Include bookings across ALL statuses (PENDING, CONFIRMED, IN_PROGRESS, COMPLETED)
     * - Filter by technicianId (only bookings where booking.technicianId == technicianId)
     * 
     * This behavior must remain unchanged after the fix to /api/v1/technician/bookings/pending
     */
    @Test
    void testTechnicianFullBookingsListReturnsAllStatuses() {
        System.out.println("\n=== Preservation Test 1: Technician Full Bookings List ===");
        System.out.println("Testing: /api/v1/technician/{technicianId}/bookings");
        System.out.println("Expected: Returns all bookings across all statuses for Tech A");
        
        // Setup: Create bookings for Tech A with different statuses
        Booking pendingBooking = createMockBooking(technicianA.getId(), BookingStatus.PENDING, "Pending booking");
        Booking confirmedBooking = createMockBooking(technicianA.getId(), BookingStatus.CONFIRMED, "Confirmed booking");
        Booking inProgressBooking = createMockBooking(technicianA.getId(), BookingStatus.IN_PROGRESS, "In progress booking");
        Booking completedBooking = createMockBooking(technicianA.getId(), BookingStatus.COMPLETED, "Completed booking");
        
        List<Booking> allBookingsForTechA = Arrays.asList(
            completedBooking,  // Ordered by createdAt DESC
            inProgressBooking,
            confirmedBooking,
            pendingBooking
        );
        
        System.out.println("\nTest Data Created:");
        System.out.println("  - 1 PENDING booking for Tech A");
        System.out.println("  - 1 CONFIRMED booking for Tech A");
        System.out.println("  - 1 IN_PROGRESS booking for Tech A");
        System.out.println("  - 1 COMPLETED booking for Tech A");
        System.out.println("  - Total: 4 bookings for Tech A");
        
        // Mock repository behavior
        when(bookingRepository.findByTechnicianIdOrderByCreatedAtDesc(technicianA.getId()))
            .thenReturn(allBookingsForTechA);
        
        // Execute: Call getTechnicianBookings() (used by /api/v1/technician/{technicianId}/bookings)
        System.out.println("\nExecuting: getTechnicianBookings(technicianId)");
        List<Booking> result = bookingService.getTechnicianBookings(technicianA.getId());
        
        System.out.println("\nResults:");
        System.out.println("  - Returned " + result.size() + " bookings");
        for (Booking booking : result) {
            System.out.println("  - Booking ID: " + booking.getId() + ", Status: " + booking.getStatus() + 
                             ", TechnicianId: " + booking.getTechnicianId());
        }
        
        // Verify: All bookings for Tech A are returned across all statuses
        assertEquals(4, result.size(), "Should return all 4 bookings for Tech A");
        
        // Verify: All returned bookings belong to Tech A
        for (Booking booking : result) {
            assertEquals(technicianA.getId(), booking.getTechnicianId(), 
                "All bookings should have technicianId == Tech A's ID");
        }
        
        // Verify: All statuses are represented
        Set<BookingStatus> statuses = new HashSet<>();
        for (Booking booking : result) {
            statuses.add(booking.getStatus());
        }
        
        assertTrue(statuses.contains(BookingStatus.PENDING), "Should include PENDING bookings");
        assertTrue(statuses.contains(BookingStatus.CONFIRMED), "Should include CONFIRMED bookings");
        assertTrue(statuses.contains(BookingStatus.IN_PROGRESS), "Should include IN_PROGRESS bookings");
        assertTrue(statuses.contains(BookingStatus.COMPLETED), "Should include COMPLETED bookings");
        
        System.out.println("\n✓ PASS: Technician full bookings list returns all statuses");
        System.out.println("✓ PRESERVATION VERIFIED: /api/v1/technician/{technicianId}/bookings unchanged");
    }

    /**
     * Property 2: Booking Acceptance Flow Preservation
     * 
     * **Validates: Requirement 3.2**
     * 
     * Test: /api/v1/technician/bookings/{bookingId}/accept
     * 
     * For any technician accepting a pending booking, the system SHALL:
     * - Validate booking is PENDING and unassigned (technicianId == NULL)
     * - Validate technician exists, is verified, and has role "technician"
     * - Check technician availability
     * - Check workload limit (max 2 active bookings)
     * - Check for time slot conflicts
     * - Assign technician to booking (set technicianId)
     * - Change status from PENDING to CONFIRMED
     * - Set confirmedAt timestamp
     * - Send notifications
     * 
     * This behavior must remain unchanged after the fix to /api/v1/technician/bookings/pending
     */
    @Test
    void testBookingAcceptanceFlowAssignsTechnicianAndConfirms() {
        System.out.println("\n=== Preservation Test 2: Booking Acceptance Flow ===");
        System.out.println("Testing: /api/v1/technician/bookings/{bookingId}/accept");
        System.out.println("Expected: Booking gets assigned to Tech A and status changes to CONFIRMED");
        
        // Setup: Create unassigned pending booking
        Booking unassignedBooking = createMockBooking(null, BookingStatus.PENDING, "Unassigned pending booking");
        
        System.out.println("\nTest Data Created:");
        System.out.println("  - 1 unassigned PENDING booking (technicianId = NULL)");
        System.out.println("  - Booking ID: " + unassignedBooking.getId());
        System.out.println("  - Status: " + unassignedBooking.getStatus());
        System.out.println("  - TechnicianId: " + unassignedBooking.getTechnicianId());
        
        // Mock repository behavior
        when(bookingRepository.findById(unassignedBooking.getId())).thenReturn(Optional.of(unassignedBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Mock technician settings (available)
        TechnicianSettings settings = new TechnicianSettings(technicianA.getId(), true);
        when(technicianSettingsRepository.findById(technicianA.getId())).thenReturn(Optional.of(settings));
        
        // Mock workload check (0 active bookings)
        when(bookingRepository.countByTechnicianIdAndStatusIn(eq(technicianA.getId()), any())).thenReturn(0L);
        
        // Mock time slot conflict check (no conflicts)
        when(bookingRepository.countByTechnicianIdAndBookingDateAndTimeSlotAndStatusIn(
            eq(technicianA.getId()), any(), any(), any())).thenReturn(0L);
        
        // Execute: Call acceptBooking() (used by /api/v1/technician/bookings/{bookingId}/accept)
        System.out.println("\nExecuting: acceptBooking(bookingId, technicianId)");
        Booking acceptedBooking = bookingService.acceptBooking(unassignedBooking.getId(), technicianA.getId());
        
        System.out.println("\nResults:");
        System.out.println("  - Booking ID: " + acceptedBooking.getId());
        System.out.println("  - Status: " + acceptedBooking.getStatus());
        System.out.println("  - TechnicianId: " + acceptedBooking.getTechnicianId());
        System.out.println("  - ConfirmedAt: " + acceptedBooking.getConfirmedAt());
        
        // Verify: Booking is assigned to Tech A
        assertEquals(technicianA.getId(), acceptedBooking.getTechnicianId(), 
            "Booking should be assigned to Tech A");
        
        // Verify: Status changed to CONFIRMED
        assertEquals(BookingStatus.CONFIRMED, acceptedBooking.getStatus(), 
            "Status should change from PENDING to CONFIRMED");
        
        // Verify: ConfirmedAt timestamp is set
        assertNotNull(acceptedBooking.getConfirmedAt(), 
            "ConfirmedAt timestamp should be set");
        
        // Verify: Notification was sent
        verify(notificationService, times(1)).notifyStatusChange(
            any(Booking.class), eq(BookingStatus.CONFIRMED), anyString());
        
        System.out.println("\n✓ PASS: Booking acceptance flow assigns technician and confirms booking");
        System.out.println("✓ PRESERVATION VERIFIED: /api/v1/technician/bookings/{bookingId}/accept unchanged");
    }

    /**
     * Property 3: Admin Booking View Preservation
     * 
     * **Validates: Requirement 3.4**
     * 
     * Test: Admin endpoints (e.g., getAllBookingsForAdmin, getRecentBookings)
     * 
     * For any admin viewing bookings, the system SHALL:
     * - Return ALL bookings in the system
     * - Include bookings regardless of technician assignment
     * - Include bookings across all statuses
     * - Show full booking details including address
     * 
     * This behavior must remain unchanged after the fix to /api/v1/technician/bookings/pending
     */
    @Test
    void testAdminBookingViewReturnsAllBookings() {
        System.out.println("\n=== Preservation Test 3: Admin Booking View ===");
        System.out.println("Testing: Admin booking endpoints");
        System.out.println("Expected: Admin sees all bookings regardless of technician assignment");
        
        // Setup: Create bookings with different assignments
        Booking booking1TechA = createMockBooking(technicianA.getId(), BookingStatus.CONFIRMED, "Booking for Tech A");
        Booking booking2Unassigned = createMockBooking(null, BookingStatus.PENDING, "Unassigned booking");
        Booking booking3TechA = createMockBooking(technicianA.getId(), BookingStatus.COMPLETED, "Another booking for Tech A");
        
        List<Booking> allBookings = Arrays.asList(
            booking3TechA,      // Ordered by createdAt DESC
            booking2Unassigned,
            booking1TechA
        );
        
        System.out.println("\nTest Data Created:");
        System.out.println("  - 1 CONFIRMED booking assigned to Tech A");
        System.out.println("  - 1 PENDING unassigned booking (technicianId = NULL)");
        System.out.println("  - 1 COMPLETED booking assigned to Tech A");
        System.out.println("  - Total: 3 bookings in system");
        
        // Mock repository behavior
        when(bookingRepository.findAllByOrderByCreatedAtDesc()).thenReturn(allBookings);
        
        // Execute: Call getAllBookingsForAdmin() (used by admin dashboard)
        System.out.println("\nExecuting: getAllBookingsForAdmin(page=0, size=10)");
        List<Map<String, Object>> result = bookingService.getAllBookingsForAdmin(0, 10);
        
        System.out.println("\nResults:");
        System.out.println("  - Returned " + result.size() + " bookings");
        for (Map<String, Object> booking : result) {
            System.out.println("  - Booking ID: " + booking.get("id") + 
                             ", Status: " + booking.get("status") + 
                             ", TechnicianName: " + booking.get("technicianName"));
        }
        
        // Verify: All bookings are returned
        assertEquals(3, result.size(), "Admin should see all 3 bookings");
        
        // Verify: Bookings include both assigned and unassigned
        boolean hasAssignedBooking = false;
        boolean hasUnassignedBooking = false;
        
        for (Map<String, Object> booking : result) {
            if (booking.get("technicianName") != null) {
                hasAssignedBooking = true;
            } else {
                hasUnassignedBooking = true;
            }
        }
        
        assertTrue(hasAssignedBooking, "Admin should see assigned bookings");
        assertTrue(hasUnassignedBooking, "Admin should see unassigned bookings");
        
        // Verify: Admin sees full details including address
        for (Map<String, Object> booking : result) {
            assertNotNull(booking.get("address"), "Admin should see address for all bookings");
            assertFalse(booking.get("address").toString().contains("will be visible"), 
                "Admin should see actual address, not placeholder");
        }
        
        System.out.println("\n✓ PASS: Admin booking view returns all bookings");
        System.out.println("✓ PRESERVATION VERIFIED: Admin endpoints unchanged");
    }

    /**
     * Property 4: Client Booking View Preservation
     * 
     * **Validates: Requirement 3.5**
     * 
     * Test: Client booking endpoints (e.g., getClientBookings)
     * 
     * For any client viewing their bookings, the system SHALL:
     * - Return ALL bookings created by that client
     * - Include bookings regardless of technician assignment
     * - Include bookings across all statuses
     * - Show full booking details including address
     * 
     * This behavior must remain unchanged after the fix to /api/v1/technician/bookings/pending
     */
    @Test
    void testClientBookingViewReturnsAllTheirBookings() {
        System.out.println("\n=== Preservation Test 4: Client Booking View ===");
        System.out.println("Testing: Client booking endpoints");
        System.out.println("Expected: Client sees all their bookings regardless of technician assignment");
        
        // Setup: Create bookings for Client X with different assignments
        Booking booking1Assigned = createMockBooking(technicianA.getId(), BookingStatus.CONFIRMED, "Assigned booking");
        booking1Assigned.setClientId(mockClient.getId());
        
        Booking booking2Unassigned = createMockBooking(null, BookingStatus.PENDING, "Unassigned booking");
        booking2Unassigned.setClientId(mockClient.getId());
        
        Booking booking3Completed = createMockBooking(technicianA.getId(), BookingStatus.COMPLETED, "Completed booking");
        booking3Completed.setClientId(mockClient.getId());
        
        List<Booking> clientBookings = Arrays.asList(
            booking3Completed,   // Ordered by createdAt DESC
            booking2Unassigned,
            booking1Assigned
        );
        
        System.out.println("\nTest Data Created:");
        System.out.println("  - 1 CONFIRMED booking assigned to Tech A (Client X)");
        System.out.println("  - 1 PENDING unassigned booking (Client X)");
        System.out.println("  - 1 COMPLETED booking assigned to Tech A (Client X)");
        System.out.println("  - Total: 3 bookings for Client X");
        
        // Mock repository behavior
        when(bookingRepository.findByClientIdOrderByCreatedAtDesc(mockClient.getId()))
            .thenReturn(clientBookings);
        
        // Execute: Call getClientBookings() (used by client dashboard)
        System.out.println("\nExecuting: getClientBookings(clientId)");
        List<Booking> result = bookingService.getClientBookings(mockClient.getId());
        
        System.out.println("\nResults:");
        System.out.println("  - Returned " + result.size() + " bookings");
        for (Booking booking : result) {
            System.out.println("  - Booking ID: " + booking.getId() + 
                             ", Status: " + booking.getStatus() + 
                             ", TechnicianId: " + booking.getTechnicianId());
        }
        
        // Verify: All client bookings are returned
        assertEquals(3, result.size(), "Client should see all 3 of their bookings");
        
        // Verify: All bookings belong to the client
        for (Booking booking : result) {
            assertEquals(mockClient.getId(), booking.getClientId(), 
                "All bookings should belong to Client X");
        }
        
        // Verify: Bookings include both assigned and unassigned
        boolean hasAssignedBooking = false;
        boolean hasUnassignedBooking = false;
        
        for (Booking booking : result) {
            if (booking.getTechnicianId() != null) {
                hasAssignedBooking = true;
            } else {
                hasUnassignedBooking = true;
            }
        }
        
        assertTrue(hasAssignedBooking, "Client should see assigned bookings");
        assertTrue(hasUnassignedBooking, "Client should see unassigned bookings");
        
        // Verify: All statuses are represented
        Set<BookingStatus> statuses = new HashSet<>();
        for (Booking booking : result) {
            statuses.add(booking.getStatus());
        }
        
        assertTrue(statuses.contains(BookingStatus.PENDING), "Should include PENDING bookings");
        assertTrue(statuses.contains(BookingStatus.CONFIRMED), "Should include CONFIRMED bookings");
        assertTrue(statuses.contains(BookingStatus.COMPLETED), "Should include COMPLETED bookings");
        
        System.out.println("\n✓ PASS: Client booking view returns all their bookings");
        System.out.println("✓ PRESERVATION VERIFIED: Client endpoints unchanged");
    }

    // Helper methods

    private User createMockTechnician(String name, String email) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName(name);
        user.setEmail(email);
        user.setRole("technician");
        user.setVerified(true);
        user.setContactNo("1234567890");
        return user;
    }

    private User createMockUser(String role, String email, boolean verified) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName("Test " + role);
        user.setEmail(email);
        user.setRole(role);
        user.setVerified(verified);
        user.setContactNo("1234567890");
        return user;
    }

    private Booking createMockBooking(UUID technicianId, BookingStatus status, String description) {
        Booking booking = new Booking();
        booking.setId(UUID.randomUUID());
        booking.setClientId(mockClient.getId());
        booking.setTechnicianId(technicianId);
        booking.setServiceType("Deep Cleaning - " + description);
        booking.setDeviceType("Laptop");
        booking.setTimeSlot("10:00 AM - 12:00 PM");
        booking.setBookingDate(LocalDate.now().plusDays(3));
        booking.setAddress("123 Test Street");
        booking.setTotalAmount(150.0);
        booking.setStatus(status);
        booking.setPaymentStatus("pending");
        return booking;
    }
}
