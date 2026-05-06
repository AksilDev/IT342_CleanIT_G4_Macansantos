package com.G4.backend;

import com.G4.backend.entity.*;
import com.G4.backend.enums.BookingStatus;
import com.G4.backend.repository.*;
import com.G4.backend.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bug Condition Exploration Test for Checklist Initialization Bug
 * 
 * Property 1: Bug Condition - Checklist Initialization Failure on Status Transition
 * 
 * CRITICAL: This test MUST FAIL on unfixed code - failure confirms the bug exists
 * DO NOT attempt to fix the test or the code when it fails
 * 
 * GOAL: Surface counterexamples that demonstrate the bug exists
 * 
 * Expected behavior (will fail on unfixed code):
 * - When a booking transitions from CONFIRMED to IN_PROGRESS
 * - The system should create 5 BookingChecklist records
 * - getBookingChecklist() should return 5 items with correct labels
 * - All items should have isChecked=false initially
 * - checklistItemRepository.findByIsActiveTrue() should return 5 records
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ChecklistInitializationBugTest {

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
    private Booking booking;

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

        // Create test service
        Service service = serviceRepository.findByName("Standard External Cleaning");
        if (service == null) {
            service = new Service("Standard External Cleaning", "Test service", 90, 200.0, true);
            service = serviceRepository.save(service);
        }

        // Create test booking with CONFIRMED status
        booking = new Booking();
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
        
        // Set status to CONFIRMED after save (PrePersist sets it to PENDING)
        booking.setStatus(BookingStatus.CONFIRMED);
        booking = bookingRepository.save(booking);
    }

    @Test
    public void testChecklistInitializationOnStatusTransition() {
        // ARRANGE: Verify preconditions
        System.out.println("\n=== Bug Condition Exploration Test ===");
        System.out.println("Testing CONFIRMED → IN_PROGRESS transition");
        
        // Verify ChecklistItem records exist in database
        List<ChecklistItem> activeItems = checklistItemRepository.findByIsActiveTrue();
        System.out.println("Active ChecklistItem records in database: " + activeItems.size());
        
        // Print details of each checklist item
        for (ChecklistItem item : activeItems) {
            System.out.println("  - " + item.getLabel() + " (isActive=" + item.getIsActive() + ")");
        }
        
        // Verify booking is in CONFIRMED status
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus(), 
            "Booking should be in CONFIRMED status before transition");
        
        // Verify no checklist items exist for this booking yet
        List<Map<String, Object>> checklistBefore = bookingService.getBookingChecklist(booking.getId());
        System.out.println("Checklist items before transition: " + checklistBefore.size());
        assertEquals(0, checklistBefore.size(), 
            "No checklist items should exist before status transition");

        // ACT: Transition booking from CONFIRMED to IN_PROGRESS
        System.out.println("\nTransitioning booking to IN_PROGRESS...");
        Booking updatedBooking = bookingService.updateBookingStatus(
            booking.getId(), 
            BookingStatus.IN_PROGRESS, 
            technician.getId(), 
            "Starting service"
        );

        // ASSERT: Verify checklist was initialized
        System.out.println("\nVerifying checklist initialization...");
        
        // Assert 1: Booking status changed to IN_PROGRESS
        assertEquals(BookingStatus.IN_PROGRESS, updatedBooking.getStatus(), 
            "Booking status should be IN_PROGRESS after transition");

        // Assert 2: getBookingChecklist() returns 5 items
        List<Map<String, Object>> checklistAfter = bookingService.getBookingChecklist(booking.getId());
        System.out.println("Checklist items after transition: " + checklistAfter.size());
        
        assertEquals(5, checklistAfter.size(), 
            "EXPECTED: 5 checklist items should be created when transitioning to IN_PROGRESS. " +
            "ACTUAL: " + checklistAfter.size() + " items found. " +
            "This confirms the bug exists if this assertion fails.");

        // Assert 3: Verify correct labels are present
        String[] expectedLabels = {
            "Verify location is valid and searchable",
            "Inspect tools for service are clean and working",
            "Client available and gives consent",
            "Inspect unit for physical damages",
            "Take a photo of unit before service starts"
        };

        for (String expectedLabel : expectedLabels) {
            boolean found = checklistAfter.stream()
                .anyMatch(item -> expectedLabel.equals(item.get("label")));
            assertTrue(found, 
                "Checklist should contain item: '" + expectedLabel + "'");
        }

        // Assert 4: All items should have isChecked=false initially
        for (Map<String, Object> item : checklistAfter) {
            Boolean isChecked = (Boolean) item.get("isChecked");
            assertFalse(isChecked, 
                "All checklist items should be unchecked initially. " +
                "Item '" + item.get("label") + "' has isChecked=" + isChecked);
        }

        // Assert 5: Verify BookingChecklist records were created in database
        List<BookingChecklist> bookingChecklists = bookingChecklistRepository.findByIdBookingId(booking.getId());
        System.out.println("BookingChecklist records in database: " + bookingChecklists.size());
        assertEquals(5, bookingChecklists.size(), 
            "5 BookingChecklist records should be created in database");

        System.out.println("\n=== Test Result ===");
        System.out.println("✓ All assertions passed - checklist initialization works correctly");
        System.out.println("If this test FAILS on unfixed code, it confirms the bug exists.");
        System.out.println("Counterexamples to document:");
        System.out.println("  - getBookingChecklist() returns empty list instead of 5 items");
        System.out.println("  - findByIsActiveTrue() returns empty list even though DataInitializer ran");
        System.out.println("  - Possible root causes: transaction isolation, persistence failure, query timing, field value issue");
    }

    @Test
    public void testChecklistItemsExistInDatabase() {
        // Diagnostic test to verify ChecklistItem records exist
        System.out.println("\n=== Diagnostic Test: ChecklistItem Existence ===");
        
        // Query all ChecklistItem records
        List<ChecklistItem> allItems = checklistItemRepository.findAll();
        System.out.println("Total ChecklistItem records: " + allItems.size());
        
        // Query active ChecklistItem records
        List<ChecklistItem> activeItems = checklistItemRepository.findByIsActiveTrue();
        System.out.println("Active ChecklistItem records: " + activeItems.size());
        
        // Print details
        for (ChecklistItem item : allItems) {
            System.out.println("  - " + item.getLabel() + " (isActive=" + item.getIsActive() + ")");
        }
        
        // Assert that 5 active items exist
        assertEquals(5, activeItems.size(), 
            "DataInitializer should create 5 active ChecklistItem records. " +
            "If this fails, the root cause is in DataInitializer.");
        
        // Verify expected labels
        String[] expectedLabels = {
            "Verify location is valid and searchable",
            "Inspect tools for service are clean and working",
            "Client available and gives consent",
            "Inspect unit for physical damages",
            "Take a photo of unit before service starts"
        };
        
        for (String expectedLabel : expectedLabels) {
            boolean found = activeItems.stream()
                .anyMatch(item -> expectedLabel.equals(item.getLabel()));
            assertTrue(found, 
                "ChecklistItem with label '" + expectedLabel + "' should exist in database");
        }
        
        System.out.println("✓ All ChecklistItem records exist correctly");
    }
}
