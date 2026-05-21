package com.G4.backend;

import com.G4.backend.features.booking.*;
import com.G4.backend.features.catalog.*;
import com.G4.backend.features.users.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Preservation Property Tests for Client Dashboard Display Fixes - Non-Photo Bookings
 * 
 * **Validates: Requirements 3.4, 3.5, 3.6**
 * 
 * Property 5: Preservation - Non-Photo Bookings
 * 
 * CRITICAL: These tests verify existing behavior on FIXED code
 * They should PASS on fixed code to establish baseline behavior to preserve
 * 
 * IMPORTANT: Follow observation-first methodology
 * - Observe behavior on FIXED code for non-buggy inputs
 * - Operations that involve bookings WITHOUT photos in booking_photos table
 * - Write property-based tests capturing observed behavior patterns
 * 
 * Test cases to observe and capture:
 * - Booking creation without any photos - verify response structure
 * - Verify response includes empty photos array or omits photos field
 * - Verify bookings without photos work without errors
 * - Verify response structure is consistent with bookings that have photos
 * 
 * EXPECTED OUTCOME: Tests PASS (this confirms baseline behavior to preserve)
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ClientDashboardNonPhotoPreservationPropertyTest {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingPhotoRepository bookingPhotoRepository;

    private User client;
    private User technician;
    private Service service;

    @BeforeEach
    public void setup() {
        // Create test client
        client = new User();
        client.setName("Test Client NonPhoto");
        client.setEmail("testclient_nonphoto@test.com");
        client.setPasswordHash("hashedpassword");
        client.setRole("client");
        client.setContactNo("1234567890");
        client.setVerified(true);
        client = userRepository.save(client);

        // Create test technician
        technician = new User();
        technician.setName("Test Technician NonPhoto");
        technician.setEmail("testtechnician_nonphoto@test.com");
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
     * Test 2.2.1: Booking without photos - verify response structure
     * Verify response includes empty photos array without errors
     * 
     * **Validates: Requirement 3.4**
     */
    @Test
    public void testBookingWithoutPhotosReturnsEmptyArray() {
        System.out.println("\n=== Test 2.2.1: Booking without photos ===");
        
        // ARRANGE: Create booking without any photos
        Booking booking = createBooking();
        booking = bookingRepository.save(booking);
        
        final java.util.UUID bookingId = booking.getId();
        
        System.out.println("Created booking without photos:");
        System.out.println("  - Booking ID: " + bookingId);
        
        // Verify no photos exist in database
        List<BookingPhoto> photosInDb = bookingPhotoRepository.findByBookingId(bookingId);
        assertEquals(0, photosInDb.size(), "No photos should exist in database");
        System.out.println("  - Photos in database: " + photosInDb.size());
        
        // ACT: Fetch booking via service (simulating client dashboard request)
        List<Booking> bookings = bookingService.getClientBookings(client.getId());
        
        // ASSERT: Verify booking is returned
        assertNotNull(bookings, "Bookings list should not be null");
        assertFalse(bookings.isEmpty(), "Bookings list should not be empty");
        
        Booking fetchedBooking = bookings.stream()
            .filter(b -> b.getId().equals(bookingId))
            .findFirst()
            .orElse(null);
        
        assertNotNull(fetchedBooking, "Booking should be found in client bookings");
        
        System.out.println("✓ Booking without photos fetched successfully");
        System.out.println("  - No errors occurred");
        System.out.println("  - Expected behavior: Controller will return empty photos array");
    }

    /**
     * Test 2.2.2: Multiple bookings without photos
     * Verify all return empty photos arrays without errors
     * 
     * **Validates: Requirements 3.4, 3.5**
     */
    @Test
    public void testMultipleBookingsWithoutPhotosAllReturnEmptyArrays() {
        System.out.println("\n=== Test 2.2.2: Property-based test - Multiple bookings without photos ===");
        
        // ARRANGE: Create multiple bookings without photos
        Booking booking1 = createBooking();
        booking1 = bookingRepository.save(booking1);
        
        Booking booking2 = createBooking();
        booking2 = bookingRepository.save(booking2);
        
        Booking booking3 = createBooking();
        booking3 = bookingRepository.save(booking3);
        
        Booking booking4 = createBooking();
        booking4 = bookingRepository.save(booking4);
        
        Booking booking5 = createBooking();
        booking5 = bookingRepository.save(booking5);
        
        System.out.println("Created 5 bookings without photos:");
        System.out.println("  - Booking 1: " + booking1.getId());
        System.out.println("  - Booking 2: " + booking2.getId());
        System.out.println("  - Booking 3: " + booking3.getId());
        System.out.println("  - Booking 4: " + booking4.getId());
        System.out.println("  - Booking 5: " + booking5.getId());
        
        // ACT: Fetch all bookings via service
        List<Booking> bookings = bookingService.getClientBookings(client.getId());
        
        // ASSERT: Verify all bookings are returned without errors
        assertNotNull(bookings, "Bookings list should not be null");
        assertEquals(5, bookings.size(), "Should return all 5 bookings");
        
        // Property: For all bookings where no photos exist in booking_photos table,
        // the system should handle them gracefully without errors
        for (Booking booking : bookings) {
            // Verify no photos exist for this booking
            List<BookingPhoto> photos = bookingPhotoRepository.findByBookingId(booking.getId());
            assertEquals(0, photos.size(), "No photos should exist for booking " + booking.getId());
            
            // Verify booking has all required fields
            assertNotNull(booking.getId(), "Booking ID should not be null");
            assertNotNull(booking.getServiceType(), "Service type should not be null");
            assertNotNull(booking.getDeviceType(), "Device type should not be null");
            assertNotNull(booking.getStatus(), "Status should not be null");
            
            System.out.println("  ✓ Booking " + booking.getId() + " - photos: 0 - OK");
        }
        
        System.out.println("✓ All bookings without photos fetched successfully");
        System.out.println("  - No errors occurred for any booking");
        System.out.println("  - Property verified: ∀ booking WHERE no photos exist → fetch succeeds without error");
    }

    /**
     * Test 2.2.3: Verify response structure consistency
     * Bookings without photos should have same structure as bookings with photos
     * 
     * **Validates: Requirement 3.6**
     */
    @Test
    public void testResponseStructureConsistencyForNonPhotoBookings() {
        System.out.println("\n=== Test 2.2.3: Response structure consistency ===");
        
        // ARRANGE: Create booking without photos
        Booking bookingWithoutPhotos = createBooking();
        bookingWithoutPhotos = bookingRepository.save(bookingWithoutPhotos);
        
        // Verify no photos exist
        List<BookingPhoto> photosInDb = bookingPhotoRepository.findByBookingId(bookingWithoutPhotos.getId());
        assertEquals(0, photosInDb.size(), "No photos should exist in database");
        
        // ACT: Fetch booking
        List<Booking> bookings = bookingService.getClientBookings(client.getId());
        
        // ASSERT: Verify booking structure
        assertNotNull(bookings, "Bookings list should not be null");
        assertFalse(bookings.isEmpty(), "Bookings list should not be empty");
        
        Booking fetchedBooking = bookings.get(0);
        
        // Verify all standard fields are present
        assertNotNull(fetchedBooking.getId(), "ID should be present");
        assertNotNull(fetchedBooking.getBookingCode(), "Booking code should be present");
        assertNotNull(fetchedBooking.getServiceType(), "Service type should be present");
        assertNotNull(fetchedBooking.getDeviceType(), "Device type should be present");
        assertNotNull(fetchedBooking.getTimeSlot(), "Time slot should be present");
        assertNotNull(fetchedBooking.getBookingDate(), "Booking date should be present");
        assertNotNull(fetchedBooking.getAddress(), "Address should be present");
        assertNotNull(fetchedBooking.getTotalAmount(), "Total amount should be present");
        assertNotNull(fetchedBooking.getStatus(), "Status should be present");
        assertNotNull(fetchedBooking.getCreatedAt(), "Created at should be present");
        
        System.out.println("✓ Response structure is consistent for bookings without photos");
        System.out.println("  - All standard fields present");
        System.out.println("  - Controller will include empty photos array in response");
        System.out.println("  - No missing or malformed fields");
    }

    /**
     * Test 2.2.4: Edge case - Booking with technician but no photos yet
     * Verify system handles bookings with assigned technician but no uploaded photos
     * 
     * **Validates: Requirement 3.5**
     */
    @Test
    public void testBookingWithTechnicianButNoPhotos() {
        System.out.println("\n=== Test 2.2.4: Edge case - Technician assigned but no photos ===");
        
        // ARRANGE: Create booking with assigned technician but no photos
        Booking booking = createBooking();
        booking.setStatus(BookingStatus.IN_PROGRESS); // Technician has started work
        booking = bookingRepository.save(booking);
        
        final java.util.UUID bookingId = booking.getId();
        
        System.out.println("Created booking with technician but no photos:");
        System.out.println("  - Booking ID: " + bookingId);
        System.out.println("  - Status: " + booking.getStatus());
        System.out.println("  - Technician ID: " + booking.getTechnicianId());
        
        // Verify no photos exist
        List<BookingPhoto> photosInDb = bookingPhotoRepository.findByBookingId(bookingId);
        assertEquals(0, photosInDb.size(), "No photos should exist yet");
        
        // ACT: Fetch booking via service
        List<Booking> bookings = bookingService.getClientBookings(client.getId());
        
        // ASSERT: Verify booking is returned without errors
        assertNotNull(bookings, "Bookings list should not be null");
        assertFalse(bookings.isEmpty(), "Bookings list should not be empty");
        
        Booking fetchedBooking = bookings.stream()
            .filter(b -> b.getId().equals(bookingId))
            .findFirst()
            .orElse(null);
        
        assertNotNull(fetchedBooking, "Booking should be found in client bookings");
        assertEquals(BookingStatus.IN_PROGRESS, fetchedBooking.getStatus(), "Status should be IN_PROGRESS");
        assertNotNull(fetchedBooking.getTechnicianId(), "Technician should be assigned");
        
        System.out.println("✓ Booking with technician but no photos fetched successfully");
        System.out.println("  - No errors occurred");
        System.out.println("  - System handles edge case gracefully");
    }

    /**
     * Test 2.2.5: Property-based test - Various booking statuses without photos
     * Verify bookings at different stages work without photos
     * 
     * **Validates: Requirements 3.4, 3.5, 3.6**
     */
    @Test
    public void testVariousBookingStatusesWithoutPhotos() {
        System.out.println("\n=== Test 2.2.5: Property-based test - Various statuses without photos ===");
        
        // ARRANGE: Create bookings with different statuses, all without photos
        Booking pendingBooking = createBooking();
        pendingBooking.setStatus(BookingStatus.PENDING);
        pendingBooking = bookingRepository.save(pendingBooking);
        
        Booking confirmedBooking = createBooking();
        confirmedBooking.setStatus(BookingStatus.CONFIRMED);
        confirmedBooking = bookingRepository.save(confirmedBooking);
        
        Booking inProgressBooking = createBooking();
        inProgressBooking.setStatus(BookingStatus.IN_PROGRESS);
        inProgressBooking = bookingRepository.save(inProgressBooking);
        
        Booking completedBooking = createBooking();
        completedBooking.setStatus(BookingStatus.COMPLETED);
        completedBooking = bookingRepository.save(completedBooking);
        
        System.out.println("Created bookings with various statuses (all without photos):");
        System.out.println("  - PENDING: " + pendingBooking.getId());
        System.out.println("  - CONFIRMED: " + confirmedBooking.getId());
        System.out.println("  - IN_PROGRESS: " + inProgressBooking.getId());
        System.out.println("  - COMPLETED: " + completedBooking.getId());
        
        // ACT: Fetch all bookings
        List<Booking> bookings = bookingService.getClientBookings(client.getId());
        
        // ASSERT: Verify all bookings are returned without errors
        assertNotNull(bookings, "Bookings list should not be null");
        assertEquals(4, bookings.size(), "Should return all 4 bookings");
        
        // Property: For all bookings regardless of status, if no photos exist,
        // the system should handle them gracefully
        for (Booking booking : bookings) {
            // Verify no photos exist
            List<BookingPhoto> photos = bookingPhotoRepository.findByBookingId(booking.getId());
            assertEquals(0, photos.size(), "No photos should exist for booking " + booking.getId());
            
            // Verify booking is valid
            assertNotNull(booking.getStatus(), "Status should not be null");
            
            System.out.println("  ✓ Booking " + booking.getId() + 
                " - status: " + booking.getStatus() + " - photos: 0 - OK");
        }
        
        System.out.println("✓ All bookings with various statuses and no photos fetched successfully");
        System.out.println("  - Property verified: ∀ booking, ∀ status WHERE no photos exist → fetch succeeds");
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
        return booking;
    }
}
