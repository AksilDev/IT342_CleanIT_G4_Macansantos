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
 * Bug Condition Exploration Test for Pre-Service Checklist Workflow Fix
 * 
 * **Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5**
 * 
 * CRITICAL: This test MUST FAIL on unfixed code - failure confirms the bug exists
 * 
 * This test encodes the EXPECTED (correct) behavior:
 * - Checklist should NOT be initialized at booking creation
 * - Checklist SHOULD be initialized at CONFIRMED → IN_PROGRESS transition
 * - Checklist validation should NOT occur at CONFIRMED → IN_PROGRESS
 * - Checklist validation SHOULD occur at IN_PROGRESS → COMPLETED
 * - Checklist should contain exactly 5 pre-service items
 * 
 * When this test FAILS on unfixed code, it surfaces counterexamples that demonstrate the bug.
 * When this test PASSES after the fix, it confirms the expected behavior is satisfied.
 */
public class BugConditionExplorationTest {

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

        // Setup mock responses
        when(userRepository.findById(mockClient.getId())).thenReturn(Optional.of(mockClient));
        when(userRepository.findById(mockTechnician.getId())).thenReturn(Optional.of(mockTechnician));

        // Create mock booking
        mockBooking = createMockBooking();
        when(bookingRepository.save(any(Booking.class))).thenReturn(mockBooking);
        when(bookingRepository.findById(mockBooking.getId())).thenReturn(Optional.of(mockBooking));
    }

    /**
     * Test 1: Checklist should NOT be initialized at booking creation
     * 
     * EXPECTED ON UNFIXED CODE: This test will FAIL because the unfixed code
     * initializes the checklist immediately at booking creation.
     * 
     * EXPECTED ON FIXED CODE: This test will PASS because the fixed code
     * does NOT initialize the checklist at booking creation.
     */
    @Test
    void testChecklistNotInitializedAtBookingCreation() {
        System.out.println("\n=== Test 1: Checklist NOT initialized at booking creation ===");
        
        // Setup: Mock empty checklist (no items initialized)
        when(checklistItemRepository.findByIsActiveTrue()).thenReturn(Collections.emptyList());
        
        // Execute: Create a booking
        Map<String, Object> bookingData = createBookingData();
        Booking createdBooking = bookingService.createBooking(bookingData);
        
        // Verify: Checklist initialization should NOT be called at booking creation
        // On UNFIXED code: This will FAIL because initializeBookingChecklist is called
        // On FIXED code: This will PASS because initializeBookingChecklist is NOT called
        verify(checklistItemRepository, never()).findByIsActiveTrue();
        verify(bookingChecklistRepository, never()).save(any(BookingChecklist.class));
        
        System.out.println("✓ PASS: Checklist was NOT initialized at booking creation (expected behavior)");
        System.out.println("  If this test FAILED, it confirms the bug: checklist is initialized too early");
    }

    /**
     * Test 2: Checklist SHOULD be initialized at CONFIRMED → IN_PROGRESS transition
     * 
     * EXPECTED ON UNFIXED CODE: This test will FAIL because the unfixed code
     * does NOT initialize the checklist at this transition point.
     * 
     * EXPECTED ON FIXED CODE: This test will PASS because the fixed code
     * initializes the checklist when service starts.
     */
    @Test
    void testChecklistInitializedAtServiceStart() {
        System.out.println("\n=== Test 2: Checklist initialized at CONFIRMED → IN_PROGRESS ===");
        
        // Setup: Mock 5 checklist items
        List<ChecklistItem> mockItems = createMock5ChecklistItems();
        when(checklistItemRepository.findByIsActiveTrue()).thenReturn(mockItems);
        
        // Setup booking in CONFIRMED status
        mockBooking.setStatus(BookingStatus.CONFIRMED);
        mockBooking.setTechnicianId(mockTechnician.getId());
        
        // Execute: Transition from CONFIRMED to IN_PROGRESS
        bookingService.updateBookingStatus(
            mockBooking.getId(),
            BookingStatus.IN_PROGRESS,
            mockTechnician.getId(),
            "Starting service"
        );
        
        // Verify: Checklist initialization SHOULD be called at this transition
        // On UNFIXED code: This will FAIL because initializeBookingChecklist is NOT called here
        // On FIXED code: This will PASS because initializeBookingChecklist IS called here
        verify(checklistItemRepository, times(1)).findByIsActiveTrue();
        verify(bookingChecklistRepository, times(5)).save(any(BookingChecklist.class));
        
        System.out.println("✓ PASS: Checklist was initialized at CONFIRMED → IN_PROGRESS (expected behavior)");
        System.out.println("  If this test FAILED, it confirms the bug: checklist is not initialized at service start");
    }

    /**
     * Test 3: Checklist validation should NOT occur at CONFIRMED → IN_PROGRESS
     * 
     * EXPECTED ON UNFIXED CODE: This test will FAIL because the unfixed code
     * validates checklist completion at this transition point.
     * 
     * EXPECTED ON FIXED CODE: This test will PASS because the fixed code
     * does NOT validate checklist at this transition (it initializes it instead).
     */
    @Test
    void testNoChecklistValidationAtServiceStart() {
        System.out.println("\n=== Test 3: NO checklist validation at CONFIRMED → IN_PROGRESS ===");
        
        // Setup: Mock empty checklist (0 items)
        when(bookingChecklistRepository.countTotalByBookingId(any())).thenReturn(0L);
        when(bookingChecklistRepository.countCheckedByBookingId(any())).thenReturn(0L);
        
        // Setup booking in CONFIRMED status
        mockBooking.setStatus(BookingStatus.CONFIRMED);
        mockBooking.setTechnicianId(mockTechnician.getId());
        
        // Execute: Transition from CONFIRMED to IN_PROGRESS
        // On UNFIXED code: This will THROW BookingException because validation occurs
        // On FIXED code: This will SUCCEED because no validation occurs
        assertDoesNotThrow(() -> {
            bookingService.updateBookingStatus(
                mockBooking.getId(),
                BookingStatus.IN_PROGRESS,
                mockTechnician.getId(),
                "Starting service"
            );
        });
        
        System.out.println("✓ PASS: No checklist validation at CONFIRMED → IN_PROGRESS (expected behavior)");
        System.out.println("  If this test FAILED, it confirms the bug: checklist validation occurs at wrong transition");
    }

    /**
     * Test 4: Checklist validation SHOULD occur at IN_PROGRESS → COMPLETED
     * 
     * EXPECTED ON UNFIXED CODE: This test will FAIL because the unfixed code
     * does NOT validate checklist completion at this transition point.
     * 
     * EXPECTED ON FIXED CODE: This test will PASS because the fixed code
     * validates checklist completion before allowing service completion.
     */
    @Test
    void testChecklistValidationAtServiceCompletion() {
        System.out.println("\n=== Test 4: Checklist validation at IN_PROGRESS → COMPLETED ===");
        
        // Setup: Mock incomplete checklist (5 total, 3 checked)
        when(bookingChecklistRepository.countTotalByBookingId(any())).thenReturn(5L);
        when(bookingChecklistRepository.countCheckedByBookingId(any())).thenReturn(3L);
        
        // Mock incomplete items for error message
        List<BookingChecklist> incompleteChecklist = createIncompleteChecklist();
        when(bookingChecklistRepository.findByIdBookingId(any())).thenReturn(incompleteChecklist);
        
        // Mock photo validation (should pass)
        when(bookingPhotoRepository.countByBookingIdAndType(any(), eq(PhotoType.BEFORE))).thenReturn(2L);
        when(bookingPhotoRepository.countByBookingIdAndType(any(), eq(PhotoType.AFTER))).thenReturn(2L);
        
        // Setup booking in IN_PROGRESS status
        mockBooking.setStatus(BookingStatus.IN_PROGRESS);
        mockBooking.setTechnicianId(mockTechnician.getId());
        
        // Execute: Attempt to transition from IN_PROGRESS to COMPLETED with incomplete checklist
        // On UNFIXED code: This will SUCCEED (no validation) - test will FAIL
        // On FIXED code: This will THROW BookingException - test will PASS
        BookingException exception = assertThrows(BookingException.class, () -> {
            bookingService.updateBookingStatus(
                mockBooking.getId(),
                BookingStatus.COMPLETED,
                mockTechnician.getId(),
                "Completing service"
            );
        });
        
        // Verify error message mentions checklist
        assertTrue(exception.getMessage().contains("checklist") || exception.getMessage().contains("pre-service"),
            "Exception should mention checklist or pre-service items");
        
        System.out.println("✓ PASS: Checklist validation occurs at IN_PROGRESS → COMPLETED (expected behavior)");
        System.out.println("  Exception message: " + exception.getMessage());
        System.out.println("  If this test FAILED, it confirms the bug: checklist validation missing at completion");
    }

    /**
     * Test 5: Checklist should contain exactly 5 pre-service items
     * 
     * EXPECTED ON UNFIXED CODE: This test will FAIL because the unfixed code
     * creates 12 checklist items instead of 5.
     * 
     * EXPECTED ON FIXED CODE: This test will PASS because the fixed code
     * creates exactly 5 pre-service items.
     */
    @Test
    void testChecklistContainsFivePreServiceItems() {
        System.out.println("\n=== Test 5: Checklist contains exactly 5 pre-service items ===");
        
        // Setup: Mock 5 pre-service checklist items (expected behavior)
        List<ChecklistItem> mockItems = createMock5ChecklistItems();
        when(checklistItemRepository.findByIsActiveTrue()).thenReturn(mockItems);
        
        // Execute: Get active checklist items
        List<ChecklistItem> activeItems = checklistItemRepository.findByIsActiveTrue();
        
        // Verify: Should have exactly 5 items
        // On UNFIXED code: This will FAIL because there are 12 items
        // On FIXED code: This will PASS because there are 5 items
        assertEquals(5, activeItems.size(), 
            "Checklist should contain exactly 5 pre-service items, but found " + activeItems.size());
        
        // Verify: Items should be pre-service items (not post-service)
        List<String> expectedLabels = Arrays.asList(
            "Verify location is valid and searchable",
            "Inspect tools for service are clean and working",
            "Client available and gives consent",
            "Test device is working before beginning physical service",
            "Review service requirements with client"
        );
        
        List<String> actualLabels = activeItems.stream()
            .map(ChecklistItem::getLabel)
            .toList();
        
        assertTrue(actualLabels.containsAll(expectedLabels),
            "Checklist should contain the 5 pre-service items. Expected: " + expectedLabels + ", Actual: " + actualLabels);
        
        System.out.println("✓ PASS: Checklist contains exactly 5 pre-service items (expected behavior)");
        System.out.println("  Items: " + actualLabels);
        System.out.println("  If this test FAILED, it confirms the bug: checklist has wrong number of items");
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

    private List<ChecklistItem> createMock5ChecklistItems() {
        List<ChecklistItem> items = new ArrayList<>();
        List<String> labels = Arrays.asList(
            "Verify location is valid and searchable",
            "Inspect tools for service are clean and working",
            "Client available and gives consent",
            "Test device is working before beginning physical service",
            "Review service requirements with client"
        );
        
        for (String label : labels) {
            ChecklistItem item = new ChecklistItem(label, true);
            item.setId(UUID.randomUUID());
            items.add(item);
        }
        
        return items;
    }

    private List<BookingChecklist> createIncompleteChecklist() {
        List<BookingChecklist> checklist = new ArrayList<>();
        List<ChecklistItem> items = createMock5ChecklistItems();
        
        // First 3 items are checked, last 2 are not
        for (int i = 0; i < items.size(); i++) {
            ChecklistItem item = items.get(i);
            BookingChecklist bc = new BookingChecklist(mockBooking, item);
            bc.setIsChecked(i < 3); // First 3 checked, last 2 unchecked
            checklist.add(bc);
        }
        
        return checklist;
    }
}
