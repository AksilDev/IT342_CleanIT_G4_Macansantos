package com.G4.backend;

import com.G4.backend.features.booking.*;
import com.G4.backend.features.catalog.*;
import com.G4.backend.features.users.*;
import com.G4.backend.features.booking.BookingStatus;
import com.G4.backend.features.booking.PhotoType;
import com.G4.backend.features.booking.*;
import com.G4.backend.features.catalog.*;
import com.G4.backend.features.users.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bug Condition Exploration Test for Booking Cascade Delete
 * 
 * **Validates: Requirements 2.1, 2.2, 2.3, 2.4**
 * 
 * Property 1: Bug Condition - Cascade Delete for Bookings with Child Records
 * 
 * CRITICAL: This test MUST FAIL on unfixed code - failure confirms the bug exists
 * DO NOT attempt to fix the test or the code when it fails
 * 
 * GOAL: Surface counterexamples that demonstrate the bug exists
 * 
 * Expected behavior (will fail on unfixed code):
 * - When a booking with associated booking_photos records is deleted
 * - The system should automatically delete all associated booking_photos records
 * - When a booking with associated booking_checklist records is deleted
 * - The system should automatically delete all associated booking_checklist records
 * - When a booking with associated booking_addons records is deleted
 * - The system should automatically delete all associated booking_addons records
 * - When a booking with all child types is deleted
 * - The system should automatically delete all child records
 * 
 * On UNFIXED code, these deletions will fail with foreign key constraint violations.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class BookingCascadeDeleteBugTest {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private BookingPhotoRepository bookingPhotoRepository;

    @Autowired
    private BookingChecklistRepository bookingChecklistRepository;

    @Autowired
    private BookingAddonRepository bookingAddonRepository;

    @Autowired
    private ChecklistItemRepository checklistItemRepository;

    @Autowired
    private AddOnRepository addOnRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private User client;
    private User technician;
    private Service service;

    @BeforeEach
    public void setup() {
        // Create test client
        client = new User();
        client.setName("Test Client");
        client.setEmail("testclient_cascade@test.com");
        client.setPasswordHash("hashedpassword");
        client.setRole("client");
        client.setContactNo("1234567890");
        client.setVerified(true);
        client = userRepository.save(client);

        // Create test technician
        technician = new User();
        technician.setName("Test Technician");
        technician.setEmail("testtechnician_cascade@test.com");
        technician.setPasswordHash("hashedpassword");
        technician.setRole("technician");
        technician.setContactNo("0987654321");
        technician.setVerified(true);
        technician = userRepository.save(technician);

        // Create test service
        service = serviceRepository.findByName("Standard External Cleaning");
        if (service == null) {
            service = new Service("Standard External Cleaning", "Test service", 90, 200.0, true);
            service = serviceRepository.save(service);
        }
    }

    /**
     * Test Case 1: Delete Booking with 2 booking_photos records
     * 
     * EXPECTED ON UNFIXED CODE: Foreign key constraint error
     * EXPECTED AFTER FIX: Booking and both photos deleted successfully
     */
    @Test
    public void testDeleteBookingWithPhotos() {
        System.out.println("\n=== Test Case 1: Delete Booking with Photos ===");
        
        // ARRANGE: Create booking with 2 photos
        Booking booking = createBooking();
        
        BookingPhoto photo1 = new BookingPhoto(booking.getId(), PhotoType.BEFORE, "https://example.com/photo1.jpg", technician.getId());
        BookingPhoto photo2 = new BookingPhoto(booking.getId(), PhotoType.AFTER, "https://example.com/photo2.jpg", technician.getId());
        bookingPhotoRepository.save(photo1);
        bookingPhotoRepository.save(photo2);
        
        // Verify photos exist
        List<BookingPhoto> photosBefore = bookingPhotoRepository.findByBookingId(booking.getId());
        assertEquals(2, photosBefore.size(), "Should have 2 photos before deletion");
        System.out.println("Created booking with 2 photos");
        
        // ACT: Attempt to delete booking
        System.out.println("Attempting to delete booking...");
        
        try {
            bookingRepository.delete(booking);
            bookingRepository.flush(); // Force immediate execution
            entityManager.clear(); // Clear persistence context to force fresh query
            
            // ASSERT: After fix, deletion should succeed
            System.out.println("✓ Booking deleted successfully (fix is working)");
            
            // Verify booking is deleted
            assertFalse(bookingRepository.existsById(booking.getId()), 
                "Booking should be deleted from database");
            
            // Verify photos are also deleted (cascade delete)
            List<BookingPhoto> photosAfter = bookingPhotoRepository.findByBookingId(booking.getId());
            assertEquals(0, photosAfter.size(), 
                "All booking_photos should be deleted via cascade delete");
            
            System.out.println("✓ All associated photos were deleted via cascade");
            
        } catch (DataIntegrityViolationException e) {
            // EXPECTED ON UNFIXED CODE: Foreign key constraint violation
            System.out.println("✗ Foreign key constraint error (bug confirmed)");
            System.out.println("Error message: " + e.getMessage());
            System.out.println("\nCounterexample found:");
            System.out.println("  - Booking ID: " + booking.getId());
            System.out.println("  - Number of photos: 2");
            System.out.println("  - Error: Unable to delete booking due to foreign key constraint from booking_photos table");
            System.out.println("\nThis confirms the bug exists - cascade delete is not configured.");
            
            // Re-throw to fail the test on unfixed code
            fail("EXPECTED FAILURE ON UNFIXED CODE: Foreign key constraint prevents deletion of booking with photos. " +
                 "After implementing the fix (adding @OneToMany with cascade=CascadeType.REMOVE), this test should pass.");
        }
    }

    /**
     * Test Case 2: Delete Booking with 3 booking_checklist records
     * 
     * EXPECTED ON UNFIXED CODE: Foreign key constraint error
     * EXPECTED AFTER FIX: Booking and all 3 checklist items deleted successfully
     */
    @Test
    public void testDeleteBookingWithChecklist() {
        System.out.println("\n=== Test Case 2: Delete Booking with Checklist ===");
        
        // ARRANGE: Create booking with 3 checklist items
        Booking booking = createBooking();
        
        // Create checklist items
        ChecklistItem item1 = new ChecklistItem("Check item 1", true);
        ChecklistItem item2 = new ChecklistItem("Check item 2", true);
        ChecklistItem item3 = new ChecklistItem("Check item 3", true);
        item1 = checklistItemRepository.save(item1);
        item2 = checklistItemRepository.save(item2);
        item3 = checklistItemRepository.save(item3);
        
        // Create booking checklist entries
        BookingChecklist bc1 = new BookingChecklist(booking, item1);
        BookingChecklist bc2 = new BookingChecklist(booking, item2);
        BookingChecklist bc3 = new BookingChecklist(booking, item3);
        bookingChecklistRepository.save(bc1);
        bookingChecklistRepository.save(bc2);
        bookingChecklistRepository.save(bc3);
        
        // Verify checklist items exist
        List<BookingChecklist> checklistBefore = bookingChecklistRepository.findByIdBookingId(booking.getId());
        assertEquals(3, checklistBefore.size(), "Should have 3 checklist items before deletion");
        System.out.println("Created booking with 3 checklist items");
        
        // ACT: Attempt to delete booking
        System.out.println("Attempting to delete booking...");
        
        try {
            bookingRepository.delete(booking);
            bookingRepository.flush(); // Force immediate execution
            entityManager.clear(); // Clear persistence context to force fresh query
            
            // ASSERT: After fix, deletion should succeed
            System.out.println("✓ Booking deleted successfully (fix is working)");
            
            // Verify booking is deleted
            assertFalse(bookingRepository.existsById(booking.getId()), 
                "Booking should be deleted from database");
            
            // Verify checklist items are also deleted (cascade delete)
            List<BookingChecklist> checklistAfter = bookingChecklistRepository.findByIdBookingId(booking.getId());
            assertEquals(0, checklistAfter.size(), 
                "All booking_checklist records should be deleted via cascade delete");
            
            System.out.println("✓ All associated checklist items were deleted via cascade");
            
        } catch (DataIntegrityViolationException e) {
            // EXPECTED ON UNFIXED CODE: Foreign key constraint violation
            System.out.println("✗ Foreign key constraint error (bug confirmed)");
            System.out.println("Error message: " + e.getMessage());
            System.out.println("\nCounterexample found:");
            System.out.println("  - Booking ID: " + booking.getId());
            System.out.println("  - Number of checklist items: 3");
            System.out.println("  - Error: Unable to delete booking due to foreign key constraint from booking_checklist table");
            System.out.println("\nThis confirms the bug exists - cascade delete is not configured.");
            
            // Re-throw to fail the test on unfixed code
            fail("EXPECTED FAILURE ON UNFIXED CODE: Foreign key constraint prevents deletion of booking with checklist. " +
                 "After implementing the fix (adding @OneToMany with cascade=CascadeType.REMOVE), this test should pass.");
        }
    }

    /**
     * Test Case 3: Delete Booking with 2 booking_addons records
     * 
     * EXPECTED ON UNFIXED CODE: Foreign key constraint error
     * EXPECTED AFTER FIX: Booking and both addon records deleted successfully
     */
    @Test
    public void testDeleteBookingWithAddons() {
        System.out.println("\n=== Test Case 3: Delete Booking with Addons ===");
        
        // ARRANGE: Create booking with 2 addons
        Booking booking = createBooking();
        
        // Create addons
        AddOn addon1 = new AddOn("Test Addon 1", "Description 1", 50.0, true);
        AddOn addon2 = new AddOn("Test Addon 2", "Description 2", 75.0, true);
        addon1 = addOnRepository.save(addon1);
        addon2 = addOnRepository.save(addon2);
        
        // Create booking addon entries
        BookingAddon ba1 = new BookingAddon(booking, addon1, addon1.getPrice());
        BookingAddon ba2 = new BookingAddon(booking, addon2, addon2.getPrice());
        bookingAddonRepository.save(ba1);
        bookingAddonRepository.save(ba2);
        
        // Verify addon records exist
        List<BookingAddon> addonsBefore = bookingAddonRepository.findByIdBookingId(booking.getId());
        assertEquals(2, addonsBefore.size(), "Should have 2 addon records before deletion");
        System.out.println("Created booking with 2 addons");
        
        // ACT: Attempt to delete booking
        System.out.println("Attempting to delete booking...");
        
        try {
            bookingRepository.delete(booking);
            bookingRepository.flush(); // Force immediate execution
            entityManager.clear(); // Clear persistence context to force fresh query
            
            // ASSERT: After fix, deletion should succeed
            System.out.println("✓ Booking deleted successfully (fix is working)");
            
            // Verify booking is deleted
            assertFalse(bookingRepository.existsById(booking.getId()), 
                "Booking should be deleted from database");
            
            // Verify addon records are also deleted (cascade delete)
            List<BookingAddon> addonsAfter = bookingAddonRepository.findByIdBookingId(booking.getId());
            assertEquals(0, addonsAfter.size(), 
                "All booking_addons records should be deleted via cascade delete");
            
            System.out.println("✓ All associated addon records were deleted via cascade");
            
        } catch (DataIntegrityViolationException e) {
            // EXPECTED ON UNFIXED CODE: Foreign key constraint violation
            System.out.println("✗ Foreign key constraint error (bug confirmed)");
            System.out.println("Error message: " + e.getMessage());
            System.out.println("\nCounterexample found:");
            System.out.println("  - Booking ID: " + booking.getId());
            System.out.println("  - Number of addons: 2");
            System.out.println("  - Error: Unable to delete booking due to foreign key constraint from booking_addons table");
            System.out.println("\nThis confirms the bug exists - cascade delete is not configured.");
            
            // Re-throw to fail the test on unfixed code
            fail("EXPECTED FAILURE ON UNFIXED CODE: Foreign key constraint prevents deletion of booking with addons. " +
                 "After implementing the fix (adding @OneToMany with cascade=CascadeType.REMOVE), this test should pass.");
        }
    }

    /**
     * Test Case 4: Delete Booking with all child types (photos, checklist, addons)
     * 
     * EXPECTED ON UNFIXED CODE: Foreign key constraint error
     * EXPECTED AFTER FIX: Booking and all child records deleted successfully
     */
    @Test
    public void testDeleteBookingWithAllChildTypes() {
        System.out.println("\n=== Test Case 4: Delete Booking with All Child Types ===");
        
        // ARRANGE: Create booking with photos, checklist, and addons
        Booking booking = createBooking();
        
        // Add 2 photos
        BookingPhoto photo1 = new BookingPhoto(booking.getId(), PhotoType.BEFORE, "https://example.com/photo1.jpg", technician.getId());
        BookingPhoto photo2 = new BookingPhoto(booking.getId(), PhotoType.AFTER, "https://example.com/photo2.jpg", technician.getId());
        bookingPhotoRepository.save(photo1);
        bookingPhotoRepository.save(photo2);
        
        // Add 3 checklist items
        ChecklistItem item1 = new ChecklistItem("Check item 1", true);
        ChecklistItem item2 = new ChecklistItem("Check item 2", true);
        ChecklistItem item3 = new ChecklistItem("Check item 3", true);
        item1 = checklistItemRepository.save(item1);
        item2 = checklistItemRepository.save(item2);
        item3 = checklistItemRepository.save(item3);
        
        BookingChecklist bc1 = new BookingChecklist(booking, item1);
        BookingChecklist bc2 = new BookingChecklist(booking, item2);
        BookingChecklist bc3 = new BookingChecklist(booking, item3);
        bookingChecklistRepository.save(bc1);
        bookingChecklistRepository.save(bc2);
        bookingChecklistRepository.save(bc3);
        
        // Add 2 addons
        AddOn addon1 = new AddOn("Test Addon 1", "Description 1", 50.0, true);
        AddOn addon2 = new AddOn("Test Addon 2", "Description 2", 75.0, true);
        addon1 = addOnRepository.save(addon1);
        addon2 = addOnRepository.save(addon2);
        
        BookingAddon ba1 = new BookingAddon(booking, addon1, addon1.getPrice());
        BookingAddon ba2 = new BookingAddon(booking, addon2, addon2.getPrice());
        bookingAddonRepository.save(ba1);
        bookingAddonRepository.save(ba2);
        
        // Verify all child records exist
        assertEquals(2, bookingPhotoRepository.findByBookingId(booking.getId()).size());
        assertEquals(3, bookingChecklistRepository.findByIdBookingId(booking.getId()).size());
        assertEquals(2, bookingAddonRepository.findByIdBookingId(booking.getId()).size());
        System.out.println("Created booking with 2 photos, 3 checklist items, and 2 addons");
        
        // ACT: Attempt to delete booking
        System.out.println("Attempting to delete booking...");
        
        try {
            bookingRepository.delete(booking);
            bookingRepository.flush(); // Force immediate execution
            entityManager.clear(); // Clear persistence context to force fresh query
            
            // ASSERT: After fix, deletion should succeed
            System.out.println("✓ Booking deleted successfully (fix is working)");
            
            // Verify booking is deleted
            assertFalse(bookingRepository.existsById(booking.getId()), 
                "Booking should be deleted from database");
            
            // Verify all child records are deleted (cascade delete)
            assertEquals(0, bookingPhotoRepository.findByBookingId(booking.getId()).size(), 
                "All booking_photos should be deleted via cascade delete");
            assertEquals(0, bookingChecklistRepository.findByIdBookingId(booking.getId()).size(), 
                "All booking_checklist records should be deleted via cascade delete");
            assertEquals(0, bookingAddonRepository.findByIdBookingId(booking.getId()).size(), 
                "All booking_addons records should be deleted via cascade delete");
            
            System.out.println("✓ All associated child records were deleted via cascade");
            System.out.println("  - 2 photos deleted");
            System.out.println("  - 3 checklist items deleted");
            System.out.println("  - 2 addon records deleted");
            
        } catch (DataIntegrityViolationException e) {
            // EXPECTED ON UNFIXED CODE: Foreign key constraint violation
            System.out.println("✗ Foreign key constraint error (bug confirmed)");
            System.out.println("Error message: " + e.getMessage());
            System.out.println("\nCounterexample found:");
            System.out.println("  - Booking ID: " + booking.getId());
            System.out.println("  - Number of photos: 2");
            System.out.println("  - Number of checklist items: 3");
            System.out.println("  - Number of addons: 2");
            System.out.println("  - Error: Unable to delete booking due to foreign key constraints from child tables");
            System.out.println("\nThis confirms the bug exists - cascade delete is not configured.");
            
            // Re-throw to fail the test on unfixed code
            fail("EXPECTED FAILURE ON UNFIXED CODE: Foreign key constraint prevents deletion of booking with all child types. " +
                 "After implementing the fix (adding @OneToMany with cascade=CascadeType.REMOVE), this test should pass.");
        }
    }

    // Helper method to create a basic booking
    private Booking createBooking() {
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
        booking.setStatus(BookingStatus.CONFIRMED);
        return bookingRepository.save(booking);
    }
}
