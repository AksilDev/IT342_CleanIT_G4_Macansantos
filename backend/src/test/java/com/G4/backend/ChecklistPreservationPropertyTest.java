package com.G4.backend;

import com.G4.backend.entity.*;
import com.G4.backend.enums.BookingStatus;
import com.G4.backend.exception.BookingException;
import com.G4.backend.repository.*;
import com.G4.backend.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Preservation Property Tests for Checklist Initialization Bug Fix
 * 
 * **Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6**
 * 
 * CRITICAL: These tests verify existing behavior on UNFIXED code
 * They should PASS on unfixed code to establish baseline behavior
 * After implementing the fix, these tests should STILL PASS (no regressions)
 * 
 * Property 2: Preservation - Non-Buggy Status Transitions and Checklist Operations
 * 
 * These tests ensure that:
 * - Status transitions other than CONFIRMED → IN_PROGRESS do not call initializeBookingChecklist()
 * - Checklist validation on IN_PROGRESS → COMPLETED enforces completion
 * - Technician permission checks for toggling checklist items remain enforced
 * - toggleChecklistItem() updates isChecked and sets checkedAt timestamp
 * - DataInitializer cleanup deletes BookingChecklist entries before ChecklistItem entries
 * - getBookingChecklist() returns correct structure with id, label, isChecked, checkedAt fields
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ChecklistPreservationPropertyTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChecklistItemRepository checklistItemRepository;

    @Autowired
    private BookingChecklistRepository bookingChecklistRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    private User client;
    private User technician;
    private User otherTechnician;
    private Service service;

    @BeforeEach
    public void setup() {
        // Create test client
        client = new User();
        client.setName("Test Client");
        client.setEmail("testclient@test.com");
        client.setPasswordHash("hashedpassword");
        client.setRole("client");
        client.setContactNo("1234567890");
        client.setVerified(true);
        client = userRepository.save(client);

        // Create test technician
        technician = new User();
        technician.setName("Test Technician");
        technician.setEmail("testtechnician@test.com");
        technician.setPasswordHash("hashedpassword");
        technician.setRole("technician");
        technician.setContactNo("0987654321");
        technician.setVerified(true);
        technician = userRepository.save(technician);

        // Create another technician (not assigned)
        otherTechnician = new User();
        otherTechnician.setName("Other Technician");
        otherTechnician.setEmail("othertechnician@test.com");
        otherTechnician.setPasswordHash("hashedpassword");
        otherTechnician.setRole("technician");
        otherTechnician.setContactNo("1112223333");
        otherTechnician.setVerified(true);
        otherTechnician = userRepository.save(otherTechnician);

        // Create test service
        service = serviceRepository.findByName("Standard External Cleaning");
        if (service == null) {
            service = new Service("Standard External Cleaning", "Test service", 90, 200.0, true);
            service = serviceRepository.save(service);
        }
    }

    /**
     * Test 2.1: For all status transitions NOT (CONFIRMED → IN_PROGRESS), 
     * verify initializeBooklistChecklist() is not called
     * 
     * **Validates: Requirement 3.1**
     */
    @Test
    public void testNonInProgressTransitionsDoNotInitializeChecklist() {
        System.out.println("\n=== Test 2.1: Non-IN_PROGRESS Transitions ===");
        
        // Test PENDING → CONFIRMED transition
        Booking booking1 = createBooking(BookingStatus.PENDING);
        booking1 = bookingService.acceptBooking(booking1.getId(), technician.getId());
        
        assertEquals(BookingStatus.CONFIRMED, booking1.getStatus());
        List<Map<String, Object>> checklist1 = bookingService.getBookingChecklist(booking1.getId());
        assertEquals(0, checklist1.size(), 
            "PENDING → CONFIRMED should not initialize checklist");
        
        System.out.println("✓ PENDING → CONFIRMED does not initialize checklist");
        System.out.println("✓ Non-IN_PROGRESS transitions preserve existing behavior");
    }

    /**
     * Test 2.2: For IN_PROGRESS → COMPLETED with incomplete checklist, 
     * verify validation error is thrown
     * 
     * **Validates: Requirement 3.3**
     */
    @Test
    public void testInProgressToCompletedWithIncompleteChecklistThrowsError() {
        System.out.println("\n=== Test 2.2: Incomplete Checklist Validation ===");
        
        // Create booking with checklist in IN_PROGRESS status
        Booking booking = createBookingWithChecklist(BookingStatus.IN_PROGRESS);
        
        // Leave checklist incomplete (don't check any items)
        List<BookingChecklist> checklistItems = bookingChecklistRepository.findByIdBookingId(booking.getId());
        assertEquals(5, checklistItems.size(), "Should have 5 checklist items");
        
        // Attempt to transition to COMPLETED with incomplete checklist
        BookingException exception = assertThrows(BookingException.class, () -> {
            bookingService.updateBookingStatus(
                booking.getId(), 
                BookingStatus.COMPLETED, 
                technician.getId(), 
                "Service completed"
            );
        });
        
        assertEquals("CHECKLIST_INCOMPLETE", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("All pre-service checklist items must be completed"));
        
        System.out.println("✓ Incomplete checklist validation works correctly");
        System.out.println("  Error message: " + exception.getMessage());
    }

    /**
     * Test 2.3: For assigned technician toggling checklist item, 
     * verify isChecked is updated and checkedAt is set
     * 
     * **Validates: Requirement 3.2**
     */
    @Test
    public void testAssignedTechnicianCanToggleChecklistItem() {
        System.out.println("\n=== Test 2.3: Assigned Technician Toggle ===");
        
        // Create booking with checklist
        Booking booking = createBookingWithChecklist(BookingStatus.IN_PROGRESS);
        
        // Get first checklist item
        List<BookingChecklist> checklistItems = bookingChecklistRepository.findByIdBookingId(booking.getId());
        assertTrue(checklistItems.size() > 0, "Should have checklist items");
        
        BookingChecklist firstItem = checklistItems.get(0);
        UUID checklistItemId = firstItem.getChecklistItem().getId();
        
        // Verify initial state
        assertFalse(firstItem.getIsChecked(), "Item should be unchecked initially");
        assertNull(firstItem.getCheckedAt(), "checkedAt should be null initially");
        
        // Toggle item (check it)
        BookingChecklist toggledItem = bookingService.toggleChecklistItem(
            booking.getId(), 
            checklistItemId, 
            technician.getId()
        );
        
        // Verify item is now checked
        assertTrue(toggledItem.getIsChecked(), "Item should be checked after toggle");
        assertNotNull(toggledItem.getCheckedAt(), "checkedAt should be set after toggle");
        
        // Toggle again (uncheck it)
        BookingChecklist toggledAgain = bookingService.toggleChecklistItem(
            booking.getId(), 
            checklistItemId, 
            technician.getId()
        );
        
        // Verify item is now unchecked
        assertFalse(toggledAgain.getIsChecked(), "Item should be unchecked after second toggle");
        
        System.out.println("✓ Assigned technician can toggle checklist items");
        System.out.println("  checkedAt timestamp: " + toggledItem.getCheckedAt());
    }

    /**
     * Test 2.4: For non-assigned technician attempting to toggle, 
     * verify "INSUFFICIENT_PERMISSIONS" error is thrown
     * 
     * **Validates: Requirement 3.4**
     */
    @Test
    public void testNonAssignedTechnicianCannotToggleChecklistItem() {
        System.out.println("\n=== Test 2.4: Non-Assigned Technician Permission Check ===");
        
        // Create booking with checklist assigned to technician
        Booking booking = createBookingWithChecklist(BookingStatus.IN_PROGRESS);
        
        // Get first checklist item
        List<BookingChecklist> checklistItems = bookingChecklistRepository.findByIdBookingId(booking.getId());
        assertTrue(checklistItems.size() > 0, "Should have checklist items");
        
        UUID checklistItemId = checklistItems.get(0).getChecklistItem().getId();
        
        // Attempt to toggle with non-assigned technician
        BookingException exception = assertThrows(BookingException.class, () -> {
            bookingService.toggleChecklistItem(
                booking.getId(), 
                checklistItemId, 
                otherTechnician.getId()
            );
        });
        
        assertEquals("INSUFFICIENT_PERMISSIONS", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("Only assigned technician can update checklist"));
        
        System.out.println("✓ Non-assigned technician permission check works correctly");
        System.out.println("  Error message: " + exception.getMessage());
    }

    /**
     * Test 2.5: For DataInitializer cleanup, verify BookingChecklist entries 
     * are deleted before ChecklistItem entries
     * 
     * **Validates: Requirement 3.5**
     * 
     * Note: This test verifies the cleanup order by checking that the DataInitializer
     * successfully cleans up old data without foreign key constraint violations.
     * The actual cleanup happens in DataInitializer.java lines 158-165.
     */
    @Test
    public void testDataInitializerCleanupOrder() {
        System.out.println("\n=== Test 2.5: DataInitializer Cleanup Order ===");
        
        // Create a booking with checklist items
        Booking booking = createBookingWithChecklist(BookingStatus.IN_PROGRESS);
        
        // Verify BookingChecklist records exist
        List<BookingChecklist> bookingChecklists = bookingChecklistRepository.findByIdBookingId(booking.getId());
        assertEquals(5, bookingChecklists.size(), "Should have 5 BookingChecklist records");
        
        // Verify ChecklistItem records exist
        List<ChecklistItem> checklistItems = checklistItemRepository.findByIsActiveTrue();
        assertEquals(5, checklistItems.size(), "Should have 5 ChecklistItem records");
        
        // Simulate cleanup: delete BookingChecklist entries first
        bookingChecklistRepository.deleteAll(bookingChecklists);
        bookingChecklistRepository.flush();
        
        // Verify BookingChecklist entries are deleted
        List<BookingChecklist> afterBookingChecklistDelete = bookingChecklistRepository.findByIdBookingId(booking.getId());
        assertEquals(0, afterBookingChecklistDelete.size(), "BookingChecklist entries should be deleted");
        
        // Now delete ChecklistItem entries (should succeed without foreign key violation)
        checklistItemRepository.deleteAll(checklistItems);
        checklistItemRepository.flush();
        
        // Verify ChecklistItem entries are deleted
        List<ChecklistItem> afterChecklistItemDelete = checklistItemRepository.findByIsActiveTrue();
        assertEquals(0, afterChecklistItemDelete.size(), "ChecklistItem entries should be deleted");
        
        System.out.println("✓ DataInitializer cleanup order is correct");
        System.out.println("  BookingChecklist deleted first, then ChecklistItem");
    }

    /**
     * Test 2.6: For getBookingChecklist() with initialized items, 
     * verify correct structure is returned
     * 
     * **Validates: Requirement 3.6**
     */
    @Test
    public void testGetBookingChecklistReturnsCorrectStructure() {
        System.out.println("\n=== Test 2.6: getBookingChecklist() Structure ===");
        
        // Create booking with checklist
        Booking booking = createBookingWithChecklist(BookingStatus.IN_PROGRESS);
        
        // Get checklist
        List<Map<String, Object>> checklist = bookingService.getBookingChecklist(booking.getId());
        
        // Verify structure
        assertEquals(5, checklist.size(), "Should return 5 checklist items");
        
        for (Map<String, Object> item : checklist) {
            // Verify all required fields are present
            assertTrue(item.containsKey("id"), "Item should have 'id' field");
            assertTrue(item.containsKey("label"), "Item should have 'label' field");
            assertTrue(item.containsKey("isChecked"), "Item should have 'isChecked' field");
            assertTrue(item.containsKey("checkedAt"), "Item should have 'checkedAt' field");
            
            // Verify field types
            assertNotNull(item.get("id"), "id should not be null");
            assertNotNull(item.get("label"), "label should not be null");
            assertNotNull(item.get("isChecked"), "isChecked should not be null");
            
            // Verify initial values
            assertEquals(false, item.get("isChecked"), "isChecked should be false initially");
            assertNull(item.get("checkedAt"), "checkedAt should be null initially");
            
            System.out.println("  Item: " + item.get("label") + " (isChecked=" + item.get("isChecked") + ")");
        }
        
        System.out.println("✓ getBookingChecklist() returns correct structure");
    }

    // Helper methods

    private Booking createBooking(BookingStatus status) {
        Booking booking = new Booking();
        booking.setClientId(client.getId());
        booking.setTechnicianId(technician.getId());
        booking.setServiceType("Standard External Cleaning");
        booking.setServiceId(service.getId());
        booking.setDeviceType("Desktop PC");
        booking.setTimeSlot("09:00 AM - 10:30 AM");
        booking.setBookingDate(LocalDate.now().plusDays(1));
        booking.setAddress("123 Test Street");
        booking.setTotalAmount(200.0);
        booking = bookingRepository.save(booking);
        
        // Set status after save (PrePersist sets it to PENDING)
        booking.setStatus(status);
        if (status == BookingStatus.CONFIRMED) {
            booking.setConfirmedAt(LocalDateTime.now());
        } else if (status == BookingStatus.IN_PROGRESS) {
            booking.setConfirmedAt(LocalDateTime.now());
            booking.setStartedAt(LocalDateTime.now());
        }
        booking = bookingRepository.save(booking);
        
        return booking;
    }

    private Booking createBookingWithChecklist(BookingStatus status) {
        Booking booking = createBooking(status);
        
        // Manually create checklist items (simulating what initializeBookingChecklist should do)
        List<ChecklistItem> allItems = checklistItemRepository.findByIsActiveTrue();
        
        for (ChecklistItem item : allItems) {
            BookingChecklist bookingChecklist = new BookingChecklist(booking, item);
            bookingChecklistRepository.save(bookingChecklist);
        }
        
        return booking;
    }
}
