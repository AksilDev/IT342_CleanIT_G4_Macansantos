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
import static org.mockito.Mockito.*;

/**
 * Bug Condition Exploration Test for Technician Booking Visibility Fix
 * 
 * **Validates: Requirements 1.1, 1.2, 1.3, 1.4**
 * 
 * CRITICAL: This test MUST FAIL on unfixed code - failure confirms the bug exists
 * DO NOT attempt to fix the test or the code when it fails
 * 
 * This test encodes the EXPECTED (correct) behavior:
 * - Technicians should ONLY see bookings where booking.technicianId == currentTechnician.id
 * - Technicians should NOT see bookings assigned to other technicians
 * - Technicians should NOT see unassigned bookings (technicianId = NULL)
 * - All returned bookings must have status == PENDING
 * 
 * When this test FAILS on unfixed code, it surfaces counterexamples that demonstrate the bug:
 * - "Tech A can see bookings assigned to Tech B"
 * - "Tech A can see unassigned bookings (technicianId = NULL)"
 * - "All technicians see all pending bookings regardless of assignment"
 * 
 * When this test PASSES after the fix, it confirms the expected behavior is satisfied.
 */
public class TechnicianBookingVisibilityBugTest {

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
    private User technicianB;
    private User technicianC;
    private User mockClient;

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

        // Create mock technicians
        technicianA = createMockTechnician("Tech A", "techA@example.com");
        technicianB = createMockTechnician("Tech B", "techB@example.com");
        technicianC = createMockTechnician("Tech C", "techC@example.com");
        mockClient = createMockUser("client", "client@example.com", true);

        // Setup mock responses for user lookups
        when(userRepository.findById(technicianA.getId())).thenReturn(Optional.of(technicianA));
        when(userRepository.findById(technicianB.getId())).thenReturn(Optional.of(technicianB));
        when(userRepository.findById(technicianC.getId())).thenReturn(Optional.of(technicianC));
        when(userRepository.findById(mockClient.getId())).thenReturn(Optional.of(mockClient));
    }

    /**
     * Property 1: Bug Condition - Technician Cross-Visibility Bug
     * 
     * Test Setup:
     * - Create 3 technicians (Tech A, Tech B, Tech C)
     * - Create 6 pending bookings:
     *   - 2 assigned to Tech A
     *   - 2 assigned to Tech B
     *   - 2 unassigned (technicianId = NULL)
     * 
     * Test Execution:
     * - Call getPendingBookingsForTechnicians() (unfixed code - no parameter)
     * 
     * Expected Behavior (on FIXED code):
     * - Method should accept technicianId parameter
     * - ONLY 2 bookings are returned (those assigned to Tech A)
     * - ALL returned bookings have technicianId == Tech A's ID
     * - Bookings assigned to Tech B are NOT visible
     * - Unassigned bookings (technicianId = NULL) are NOT visible
     * 
     * Expected Counterexamples (on UNFIXED code):
     * - Method does NOT accept technicianId parameter (compilation error demonstrates bug)
     * - Returns ALL unassigned pending bookings (technicianId = NULL)
     * - All technicians see the same unassigned bookings
     */
    @Test
    void testTechnicianCanOnlySeeTheirOwnAssignedPendingBookings() {
        System.out.println("\n=== Bug Condition Exploration: Technician Booking Visibility ===");
        System.out.println("Testing that technicians can ONLY see bookings assigned to them");
        
        // Setup: Create 6 pending bookings with different assignments
        List<Booking> allPendingBookings = new ArrayList<>();
        
        // 2 bookings assigned to Tech A (PENDING status)
        Booking booking1TechA = createMockBooking(technicianA.getId(), BookingStatus.PENDING, "Booking 1 for Tech A");
        Booking booking2TechA = createMockBooking(technicianA.getId(), BookingStatus.PENDING, "Booking 2 for Tech A");
        
        // 2 bookings assigned to Tech B (PENDING status)
        Booking booking1TechB = createMockBooking(technicianB.getId(), BookingStatus.PENDING, "Booking 1 for Tech B");
        Booking booking2TechB = createMockBooking(technicianB.getId(), BookingStatus.PENDING, "Booking 2 for Tech B");
        
        // 2 unassigned bookings (technicianId = NULL, PENDING status)
        // These are the bookings that the UNFIXED code returns
        Booking unassignedBooking1 = createMockBooking(null, BookingStatus.PENDING, "Unassigned Booking 1");
        Booking unassignedBooking2 = createMockBooking(null, BookingStatus.PENDING, "Unassigned Booking 2");
        
        System.out.println("\nTest Data Created:");
        System.out.println("  - 2 bookings assigned to Tech A (ID: " + technicianA.getId() + ")");
        System.out.println("  - 2 bookings assigned to Tech B (ID: " + technicianB.getId() + ")");
        System.out.println("  - 2 unassigned bookings (technicianId = NULL)");
        System.out.println("  - Total: 6 pending bookings");
        
        // Mock repository behavior for FIXED code
        // The fixed code calls findByTechnicianIdAndStatusOrderByCreatedAtAsc(technicianId, PENDING)
        // This returns ONLY bookings assigned to the specified technician with PENDING status
        when(bookingRepository.findByTechnicianIdAndStatusOrderByCreatedAtAsc(technicianA.getId(), BookingStatus.PENDING))
            .thenReturn(Arrays.asList(booking1TechA, booking2TechA));
        
        when(bookingRepository.findByTechnicianIdAndStatusOrderByCreatedAtAsc(technicianB.getId(), BookingStatus.PENDING))
            .thenReturn(Arrays.asList(booking1TechB, booking2TechB));
        
        // Execute: Call getPendingBookingsForTechnicians() with Tech A's ID (FIXED code)
        System.out.println("\nExecuting: getPendingBookingsForTechnicians(technicianA.getId()) [FIXED CODE]");
        
        List<Map<String, Object>> resultTechA = bookingService.getPendingBookingsForTechnicians(technicianA.getId());
        
        System.out.println("\nResults for Tech A:");
        System.out.println("  - Returned " + resultTechA.size() + " bookings");
        for (Map<String, Object> booking : resultTechA) {
            System.out.println("  - Booking ID: " + booking.get("id") + ", technicianId: " + booking.get("technicianId"));
        }
        
        // VERIFY EXPECTED BEHAVIOR ON FIXED CODE:
        System.out.println("\n=== VERIFYING FIX ===");
        
        // Assertion 1: Tech A should see ONLY 2 bookings
        assertEquals(2, resultTechA.size(), 
            "Tech A should see exactly 2 pending bookings (their assigned bookings)");
        System.out.println("✓ Tech A sees exactly 2 bookings (correct count)");
        
        // Assertion 2: ALL returned bookings should have technicianId == Tech A's ID
        for (Map<String, Object> booking : resultTechA) {
            String technicianIdStr = (String) booking.get("technicianId");
            assertEquals(technicianA.getId().toString(), technicianIdStr,
                "All bookings returned for Tech A should have technicianId == Tech A's ID");
        }
        System.out.println("✓ All returned bookings have technicianId == Tech A's ID");
        
        // Assertion 3: ALL returned bookings should have status == PENDING (lowercase in response)
        for (Map<String, Object> booking : resultTechA) {
            assertEquals("pending", booking.get("status"),
                "All bookings returned should have status == pending");
        }
        System.out.println("✓ All returned bookings have status == PENDING");
        
        // Verify Tech B isolation: Tech B should see ONLY their 2 bookings
        System.out.println("\nExecuting: getPendingBookingsForTechnicians(technicianB.getId()) [FIXED CODE]");
        List<Map<String, Object>> resultTechB = bookingService.getPendingBookingsForTechnicians(technicianB.getId());
        
        System.out.println("\nResults for Tech B:");
        System.out.println("  - Returned " + resultTechB.size() + " bookings");
        
        assertEquals(2, resultTechB.size(), 
            "Tech B should see exactly 2 pending bookings (their assigned bookings)");
        System.out.println("✓ Tech B sees exactly 2 bookings (correct count)");
        
        for (Map<String, Object> booking : resultTechB) {
            String technicianIdStr = (String) booking.get("technicianId");
            assertEquals(technicianB.getId().toString(), technicianIdStr,
                "All bookings returned for Tech B should have technicianId == Tech B's ID");
        }
        System.out.println("✓ All returned bookings have technicianId == Tech B's ID");
        
        // Verify that Tech A does NOT see Tech B's bookings
        System.out.println("\n=== VERIFYING ISOLATION ===");
        System.out.println("✓ Tech A does NOT see Tech B's bookings");
        System.out.println("✓ Tech B does NOT see Tech A's bookings");
        System.out.println("✓ Neither technician sees unassigned bookings (technicianId = NULL)");
        
        System.out.println("\n=== TEST RESULT ===");
        System.out.println("✓ Fix verified successfully!");
        System.out.println("  The fixed code exhibits the expected correct behavior:");
        System.out.println("  - Technicians see ONLY bookings assigned to them");
        System.out.println("  - Bookings are filtered by technicianId");
        System.out.println("  - Each technician's view is isolated from others");
        System.out.println("  - Unassigned bookings are NOT visible to technicians");
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
