package com.G4.backend;

import com.G4.backend.entity.*;
import com.G4.backend.enums.BookingStatus;
import com.G4.backend.enums.PhotoType;
import com.G4.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Preservation Property Tests for Booking Cascade Delete Fix
 * 
 * **Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5**
 * 
 * Property 2: Preservation - Non-Deletion Operations
 * 
 * CRITICAL: These tests verify existing behavior on UNFIXED code
 * They should PASS on unfixed code to establish baseline behavior
 * After implementing the fix, these tests should STILL PASS (no regressions)
 * 
 * IMPORTANT: Follow observation-first methodology
 * - Observe behavior on UNFIXED code for non-buggy inputs
 * - Operations that don't involve deleting bookings with child records
 * - Write property-based tests capturing observed behavior patterns
 * 
 * Test cases to observe and capture:
 * - Booking creation with child records (photos, checklist, addons) - verify creation works correctly
 * - Querying bookings and their associated child records - verify correct data is returned
 * - Updating booking fields without deletion - verify updates work correctly
 * - Independent deletion of child records (without deleting parent booking) - verify child deletion works
 * - Deleting bookings with NO child records - verify deletion works (edge case that already works)
 * 
 * EXPECTED OUTCOME: Tests PASS (this confirms baseline behavior to preserve)
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class BookingCascadeDeletePreservationPropertyTest {

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

    private User client;
    private User technician;
    private Service service;

    @BeforeEach
    public void setup() {
        // Create test client
        client = new User();
        client.setName("Test Client Preservation");
        client.setEmail("testclient_preservation@test.com");
        client.setPasswordHash("hashedpassword");
        client.setRole("client");
        client.setContactNo("1234567890");
        client.setVerified(true);
        client = userRepository.save(client);

        // Create test technician
        technician = new User();
        technician.setName("Test Technician Preservation");
        technician.setEmail("testtechnician_preservation@test.com");
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
     * Test 2.1: Booking creation with child records (photos, checklist, addons)
     * Verify creation works correctly
     * 
     * **Validates: Requirement 3.2**
     */
    @Test
    public void testBookingCreationWithChildRecords() {
        System.out.println("\n=== Test 2.1: Booking Creation with Child Records ===");
        
        // ARRANGE & ACT: Create booking
        Booking booking = createBooking();
        
        // Add 2 photos
        BookingPhoto photo1 = new BookingPhoto(booking.getId(), PhotoType.BEFORE, "https://example.com/photo1.jpg", technician.getId());
        BookingPhoto photo2 = new BookingPhoto(booking.getId(), PhotoType.AFTER, "https://example.com/photo2.jpg", technician.getId());
        photo1 = bookingPhotoRepository.save(photo1);
        photo2 = bookingPhotoRepository.save(photo2);
        
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
        bc1 = bookingChecklistRepository.save(bc1);
        bc2 = bookingChecklistRepository.save(bc2);
        bc3 = bookingChecklistRepository.save(bc3);
        
        // Add 2 addons
        AddOn addon1 = new AddOn("Test Addon 1", "Description 1", 50.0, true);
        AddOn addon2 = new AddOn("Test Addon 2", "Description 2", 75.0, true);
        addon1 = addOnRepository.save(addon1);
        addon2 = addOnRepository.save(addon2);
        
        BookingAddon ba1 = new BookingAddon(booking, addon1, addon1.getPrice());
        BookingAddon ba2 = new BookingAddon(booking, addon2, addon2.getPrice());
        ba1 = bookingAddonRepository.save(ba1);
        ba2 = bookingAddonRepository.save(ba2);
        
        // ASSERT: Verify all records were created successfully
        assertTrue(bookingRepository.existsById(booking.getId()), 
            "Booking should be created successfully");
        
        List<BookingPhoto> photos = bookingPhotoRepository.findByBookingId(booking.getId());
        assertEquals(2, photos.size(), "Should have 2 photos");
        
        List<BookingChecklist> checklist = bookingChecklistRepository.findByIdBookingId(booking.getId());
        assertEquals(3, checklist.size(), "Should have 3 checklist items");
        
        List<BookingAddon> addons = bookingAddonRepository.findByIdBookingId(booking.getId());
        assertEquals(2, addons.size(), "Should have 2 addons");
        
        System.out.println("✓ Booking creation with child records works correctly");
        System.out.println("  - Booking ID: " + booking.getId());
        System.out.println("  - Photos: " + photos.size());
        System.out.println("  - Checklist items: " + checklist.size());
        System.out.println("  - Addons: " + addons.size());
    }

    /**
     * Test 2.2: Querying bookings and their associated child records
     * Verify correct data is returned
     * 
     * **Validates: Requirement 3.3**
     */
    @Test
    public void testQueryingBookingsAndChildRecords() {
        System.out.println("\n=== Test 2.2: Querying Bookings and Child Records ===");
        
        // ARRANGE: Create booking with child records
        Booking booking = createBooking();
        
        BookingPhoto photo = new BookingPhoto(booking.getId(), PhotoType.BEFORE, "https://example.com/photo.jpg", technician.getId());
        bookingPhotoRepository.save(photo);
        
        ChecklistItem item = new ChecklistItem("Check item", true);
        item = checklistItemRepository.save(item);
        BookingChecklist bc = new BookingChecklist(booking, item);
        bookingChecklistRepository.save(bc);
        
        AddOn addon = new AddOn("Test Addon", "Description", 50.0, true);
        addon = addOnRepository.save(addon);
        BookingAddon ba = new BookingAddon(booking, addon, addon.getPrice());
        bookingAddonRepository.save(ba);
        
        // ACT: Query booking and child records
        Booking queriedBooking = bookingRepository.findById(booking.getId()).orElse(null);
        List<BookingPhoto> queriedPhotos = bookingPhotoRepository.findByBookingId(booking.getId());
        List<BookingChecklist> queriedChecklist = bookingChecklistRepository.findByIdBookingId(booking.getId());
        List<BookingAddon> queriedAddons = bookingAddonRepository.findByIdBookingId(booking.getId());
        
        // ASSERT: Verify correct data is returned
        assertNotNull(queriedBooking, "Booking should be found");
        assertEquals(booking.getId(), queriedBooking.getId(), "Booking ID should match");
        
        assertEquals(1, queriedPhotos.size(), "Should return 1 photo");
        assertEquals(photo.getId(), queriedPhotos.get(0).getId(), "Photo ID should match");
        assertEquals(PhotoType.BEFORE, queriedPhotos.get(0).getType(), "Photo type should match");
        
        assertEquals(1, queriedChecklist.size(), "Should return 1 checklist item");
        assertEquals(item.getId(), queriedChecklist.get(0).getChecklistItem().getId(), "Checklist item ID should match");
        
        assertEquals(1, queriedAddons.size(), "Should return 1 addon");
        assertEquals(addon.getId(), queriedAddons.get(0).getAddOn().getId(), "Addon ID should match");
        assertEquals(50.0, queriedAddons.get(0).getPriceAtBooking(), "Addon price should match");
        
        System.out.println("✓ Querying bookings and child records returns correct data");
        System.out.println("  - Booking found: " + queriedBooking.getId());
        System.out.println("  - Photos found: " + queriedPhotos.size());
        System.out.println("  - Checklist items found: " + queriedChecklist.size());
        System.out.println("  - Addons found: " + queriedAddons.size());
    }

    /**
     * Test 2.3: Updating booking fields without deletion
     * Verify updates work correctly
     * 
     * **Validates: Requirement 3.4**
     */
    @Test
    public void testUpdatingBookingFieldsWithoutDeletion() {
        System.out.println("\n=== Test 2.3: Updating Booking Fields Without Deletion ===");
        
        // ARRANGE: Create booking with child records
        Booking booking = createBooking();
        
        BookingPhoto photo = new BookingPhoto(booking.getId(), PhotoType.BEFORE, "https://example.com/photo.jpg", technician.getId());
        bookingPhotoRepository.save(photo);
        
        ChecklistItem item = new ChecklistItem("Check item", true);
        item = checklistItemRepository.save(item);
        BookingChecklist bc = new BookingChecklist(booking, item);
        bookingChecklistRepository.save(bc);
        
        AddOn addon = new AddOn("Test Addon", "Description", 50.0, true);
        addon = addOnRepository.save(addon);
        BookingAddon ba = new BookingAddon(booking, addon, addon.getPrice());
        bookingAddonRepository.save(ba);
        
        // Store original values
        String originalAddress = booking.getAddress();
        Double originalAmount = booking.getTotalAmount();
        BookingStatus originalStatus = booking.getStatus();
        
        // ACT: Update booking fields
        booking.setAddress("456 New Address");
        booking.setTotalAmount(300.0);
        booking.setStatus(BookingStatus.IN_PROGRESS);
        booking.setSpecialInstructions("Updated instructions");
        booking = bookingRepository.save(booking);
        
        // ASSERT: Verify updates were applied
        Booking updatedBooking = bookingRepository.findById(booking.getId()).orElse(null);
        assertNotNull(updatedBooking, "Booking should still exist");
        assertEquals("456 New Address", updatedBooking.getAddress(), "Address should be updated");
        assertEquals(300.0, updatedBooking.getTotalAmount(), "Total amount should be updated");
        assertEquals(BookingStatus.IN_PROGRESS, updatedBooking.getStatus(), "Status should be updated");
        assertEquals("Updated instructions", updatedBooking.getSpecialInstructions(), "Special instructions should be updated");
        
        // Verify child records are unaffected
        List<BookingPhoto> photos = bookingPhotoRepository.findByBookingId(booking.getId());
        assertEquals(1, photos.size(), "Photos should remain unchanged");
        
        List<BookingChecklist> checklist = bookingChecklistRepository.findByIdBookingId(booking.getId());
        assertEquals(1, checklist.size(), "Checklist should remain unchanged");
        
        List<BookingAddon> addons = bookingAddonRepository.findByIdBookingId(booking.getId());
        assertEquals(1, addons.size(), "Addons should remain unchanged");
        
        System.out.println("✓ Updating booking fields without deletion works correctly");
        System.out.println("  - Original address: " + originalAddress + " → New address: " + updatedBooking.getAddress());
        System.out.println("  - Original amount: " + originalAmount + " → New amount: " + updatedBooking.getTotalAmount());
        System.out.println("  - Original status: " + originalStatus + " → New status: " + updatedBooking.getStatus());
        System.out.println("  - Child records remain unchanged");
    }

    /**
     * Test 2.4: Independent deletion of child records (without deleting parent booking)
     * Verify child deletion works
     * 
     * **Validates: Requirement 3.5**
     */
    @Test
    public void testIndependentDeletionOfChildRecords() {
        System.out.println("\n=== Test 2.4: Independent Deletion of Child Records ===");
        
        // ARRANGE: Create booking with child records
        Booking booking = createBooking();
        
        BookingPhoto photo1 = new BookingPhoto(booking.getId(), PhotoType.BEFORE, "https://example.com/photo1.jpg", technician.getId());
        BookingPhoto photo2 = new BookingPhoto(booking.getId(), PhotoType.AFTER, "https://example.com/photo2.jpg", technician.getId());
        photo1 = bookingPhotoRepository.save(photo1);
        photo2 = bookingPhotoRepository.save(photo2);
        
        ChecklistItem item1 = new ChecklistItem("Check item 1", true);
        ChecklistItem item2 = new ChecklistItem("Check item 2", true);
        item1 = checklistItemRepository.save(item1);
        item2 = checklistItemRepository.save(item2);
        
        BookingChecklist bc1 = new BookingChecklist(booking, item1);
        BookingChecklist bc2 = new BookingChecklist(booking, item2);
        bc1 = bookingChecklistRepository.save(bc1);
        bc2 = bookingChecklistRepository.save(bc2);
        
        AddOn addon1 = new AddOn("Test Addon 1", "Description 1", 50.0, true);
        AddOn addon2 = new AddOn("Test Addon 2", "Description 2", 75.0, true);
        addon1 = addOnRepository.save(addon1);
        addon2 = addOnRepository.save(addon2);
        
        BookingAddon ba1 = new BookingAddon(booking, addon1, addon1.getPrice());
        BookingAddon ba2 = new BookingAddon(booking, addon2, addon2.getPrice());
        ba1 = bookingAddonRepository.save(ba1);
        ba2 = bookingAddonRepository.save(ba2);
        
        // ACT: Delete individual child records (not the booking)
        bookingPhotoRepository.delete(photo1);
        bookingChecklistRepository.delete(bc1);
        bookingAddonRepository.delete(ba1);
        
        // ASSERT: Verify child records were deleted
        List<BookingPhoto> remainingPhotos = bookingPhotoRepository.findByBookingId(booking.getId());
        assertEquals(1, remainingPhotos.size(), "Should have 1 photo remaining");
        assertEquals(photo2.getId(), remainingPhotos.get(0).getId(), "Remaining photo should be photo2");
        
        List<BookingChecklist> remainingChecklist = bookingChecklistRepository.findByIdBookingId(booking.getId());
        assertEquals(1, remainingChecklist.size(), "Should have 1 checklist item remaining");
        assertEquals(item2.getId(), remainingChecklist.get(0).getChecklistItem().getId(), "Remaining checklist item should be item2");
        
        List<BookingAddon> remainingAddons = bookingAddonRepository.findByIdBookingId(booking.getId());
        assertEquals(1, remainingAddons.size(), "Should have 1 addon remaining");
        assertEquals(addon2.getId(), remainingAddons.get(0).getAddOn().getId(), "Remaining addon should be addon2");
        
        // Verify booking still exists
        assertTrue(bookingRepository.existsById(booking.getId()), 
            "Booking should still exist after deleting child records");
        
        System.out.println("✓ Independent deletion of child records works correctly");
        System.out.println("  - Deleted 1 photo, 1 remaining");
        System.out.println("  - Deleted 1 checklist item, 1 remaining");
        System.out.println("  - Deleted 1 addon, 1 remaining");
        System.out.println("  - Booking still exists: " + booking.getId());
    }

    /**
     * Test 2.5: Deleting bookings with NO child records
     * Verify deletion works (edge case that already works)
     * 
     * **Validates: Requirement 3.1**
     */
    @Test
    public void testDeletingBookingsWithNoChildRecords() {
        System.out.println("\n=== Test 2.5: Deleting Bookings with NO Child Records ===");
        
        // ARRANGE: Create booking WITHOUT child records
        Booking booking = createBooking();
        
        // Verify no child records exist
        assertEquals(0, bookingPhotoRepository.findByBookingId(booking.getId()).size(), 
            "Should have no photos");
        assertEquals(0, bookingChecklistRepository.findByIdBookingId(booking.getId()).size(), 
            "Should have no checklist items");
        assertEquals(0, bookingAddonRepository.findByIdBookingId(booking.getId()).size(), 
            "Should have no addons");
        
        UUID bookingId = booking.getId();
        
        // ACT: Delete booking
        bookingRepository.delete(booking);
        bookingRepository.flush();
        
        // ASSERT: Verify booking was deleted successfully
        assertFalse(bookingRepository.existsById(bookingId), 
            "Booking should be deleted successfully");
        
        System.out.println("✓ Deleting bookings with no child records works correctly");
        System.out.println("  - Booking ID: " + bookingId);
        System.out.println("  - Deletion successful (no child records to cascade)");
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
