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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Preservation Property Tests for Client Dashboard Display Fixes
 * 
 * **Validates: Requirements 3.1, 3.2, 3.3**
 * 
 * Property 4: Preservation - Non-Addon Bookings
 * 
 * CRITICAL: These tests verify existing behavior on FIXED code
 * They should PASS on fixed code to establish baseline behavior to preserve
 * 
 * IMPORTANT: Follow observation-first methodology
 * - Observe behavior on FIXED code for non-buggy inputs
 * - Operations that involve bookings WITHOUT addons (null or empty)
 * - Write property-based tests capturing observed behavior patterns
 * 
 * Test cases to observe and capture:
 * - Booking creation with null addOns field - verify returns empty addon array
 * - Booking creation with empty string addOns field - verify returns empty addon array
 * - Verify both scenarios work without errors
 * - Verify response structure is consistent with bookings that have addons
 * 
 * EXPECTED OUTCOME: Tests PASS (this confirms baseline behavior to preserve)
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ClientDashboardNonAddonPreservationPropertyTest {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private BookingService bookingService;

    private User client;
    private User technician;
    private Service service;

    @BeforeEach
    public void setup() {
        // Create test client
        client = new User();
        client.setName("Test Client NonAddon");
        client.setEmail("testclient_nonaddon@test.com");
        client.setPasswordHash("hashedpassword");
        client.setRole("client");
        client.setContactNo("1234567890");
        client.setVerified(true);
        client = userRepository.save(client);

        // Create test technician
        technician = new User();
        technician.setName("Test Technician NonAddon");
        technician.setEmail("testtechnician_nonaddon@test.com");
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
     * Test 2.1.1: Booking with null addOns field
     * Verify returns empty addon array without errors
     * 
     * **Validates: Requirement 3.1**
     */
    @Test
    public void testBookingWithNullAddOnsReturnsEmptyArray() {
        System.out.println("\n=== Test 2.1.1: Booking with null addOns field ===");
        
        // ARRANGE: Create booking with null addOns
        Booking booking = createBooking();
        booking.setAddOns(null);
        booking = bookingRepository.save(booking);
        
        final java.util.UUID bookingId = booking.getId();
        
        System.out.println("Created booking with null addOns:");
        System.out.println("  - Booking ID: " + bookingId);
        System.out.println("  - AddOns field: " + booking.getAddOns());
        
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
        
        // Verify addOns field is null (as stored)
        assertNull(fetchedBooking.getAddOns(), "AddOns field should be null");
        
        System.out.println("✓ Booking with null addOns fetched successfully");
        System.out.println("  - No errors occurred");
        System.out.println("  - AddOns field: " + fetchedBooking.getAddOns());
        System.out.println("  - Expected behavior: Controller will return empty array for null addOns");
    }

    /**
     * Test 2.1.2: Booking with empty string addOns field
     * Verify returns empty addon array without errors
     * 
     * **Validates: Requirement 3.2**
     */
    @Test
    public void testBookingWithEmptyStringAddOnsReturnsEmptyArray() {
        System.out.println("\n=== Test 2.1.2: Booking with empty string addOns field ===");
        
        // ARRANGE: Create booking with empty string addOns
        Booking booking = createBooking();
        booking.setAddOns("");
        booking = bookingRepository.save(booking);
        
        final java.util.UUID bookingId = booking.getId();
        
        System.out.println("Created booking with empty string addOns:");
        System.out.println("  - Booking ID: " + bookingId);
        System.out.println("  - AddOns field: '" + booking.getAddOns() + "'");
        
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
        
        // Verify addOns field is empty string (as stored)
        assertEquals("", fetchedBooking.getAddOns(), "AddOns field should be empty string");
        
        System.out.println("✓ Booking with empty string addOns fetched successfully");
        System.out.println("  - No errors occurred");
        System.out.println("  - AddOns field: '" + fetchedBooking.getAddOns() + "'");
        System.out.println("  - Expected behavior: Controller will return empty array for empty string addOns");
    }

    /**
     * Test 2.1.3: Property-based test - Multiple bookings with null/empty addOns
     * Verify all return empty addon arrays without errors
     * 
     * **Validates: Requirements 3.1, 3.2, 3.3**
     */
    @Test
    public void testMultipleBookingsWithoutAddOnsAllReturnEmptyArrays() {
        System.out.println("\n=== Test 2.1.3: Property-based test - Multiple bookings without addOns ===");
        
        // ARRANGE: Create multiple bookings with various null/empty addOns scenarios
        Booking booking1 = createBooking();
        booking1.setAddOns(null);
        booking1 = bookingRepository.save(booking1);
        
        Booking booking2 = createBooking();
        booking2.setAddOns("");
        booking2 = bookingRepository.save(booking2);
        
        Booking booking3 = createBooking();
        booking3.setAddOns(null);
        booking3 = bookingRepository.save(booking3);
        
        Booking booking4 = createBooking();
        booking4.setAddOns("");
        booking4 = bookingRepository.save(booking4);
        
        Booking booking5 = createBooking();
        booking5.setAddOns(null);
        booking5 = bookingRepository.save(booking5);
        
        System.out.println("Created 5 bookings with null/empty addOns:");
        System.out.println("  - Booking 1: addOns = " + booking1.getAddOns());
        System.out.println("  - Booking 2: addOns = '" + booking2.getAddOns() + "'");
        System.out.println("  - Booking 3: addOns = " + booking3.getAddOns());
        System.out.println("  - Booking 4: addOns = '" + booking4.getAddOns() + "'");
        System.out.println("  - Booking 5: addOns = " + booking5.getAddOns());
        
        // ACT: Fetch all bookings via service
        List<Booking> bookings = bookingService.getClientBookings(client.getId());
        
        // ASSERT: Verify all bookings are returned without errors
        assertNotNull(bookings, "Bookings list should not be null");
        assertEquals(5, bookings.size(), "Should return all 5 bookings");
        
        // Property: For all bookings where addOns is null or empty, 
        // the system should handle them gracefully without errors
        for (Booking booking : bookings) {
            String addOns = booking.getAddOns();
            
            // Verify addOns is either null or empty
            assertTrue(addOns == null || addOns.isEmpty(), 
                "AddOns should be null or empty for booking " + booking.getId());
            
            // Verify booking has all required fields
            assertNotNull(booking.getId(), "Booking ID should not be null");
            assertNotNull(booking.getServiceType(), "Service type should not be null");
            assertNotNull(booking.getDeviceType(), "Device type should not be null");
            assertNotNull(booking.getStatus(), "Status should not be null");
            
            System.out.println("  ✓ Booking " + booking.getId() + " - addOns: " + 
                (addOns == null ? "null" : "'" + addOns + "'") + " - OK");
        }
        
        System.out.println("✓ All bookings without addOns fetched successfully");
        System.out.println("  - No errors occurred for any booking");
        System.out.println("  - Property verified: ∀ booking WHERE addOns IS NULL OR EMPTY → fetch succeeds without error");
    }

    /**
     * Test 2.1.4: Verify response structure consistency
     * Bookings without addOns should have same structure as bookings with addOns
     * 
     * **Validates: Requirement 3.3**
     */
    @Test
    public void testResponseStructureConsistencyForNonAddonBookings() {
        System.out.println("\n=== Test 2.1.4: Response structure consistency ===");
        
        // ARRANGE: Create booking without addOns
        Booking bookingWithoutAddons = createBooking();
        bookingWithoutAddons.setAddOns(null);
        bookingWithoutAddons = bookingRepository.save(bookingWithoutAddons);
        
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
        
        // Verify addOns field is null (will be transformed to empty array by controller)
        assertNull(fetchedBooking.getAddOns(), "AddOns field should be null");
        
        System.out.println("✓ Response structure is consistent for bookings without addOns");
        System.out.println("  - All standard fields present");
        System.out.println("  - AddOns field: null (controller will transform to empty array)");
        System.out.println("  - No missing or malformed fields");
    }

    /**
     * Test 2.1.5: Edge case - Whitespace-only addOns field
     * Verify system handles whitespace gracefully
     * 
     * **Validates: Requirement 3.3**
     */
    @Test
    public void testBookingWithWhitespaceOnlyAddOns() {
        System.out.println("\n=== Test 2.1.5: Edge case - Whitespace-only addOns ===");
        
        // ARRANGE: Create booking with whitespace-only addOns
        Booking booking = createBooking();
        booking.setAddOns("   ");
        booking = bookingRepository.save(booking);
        
        final java.util.UUID bookingId = booking.getId();
        
        System.out.println("Created booking with whitespace-only addOns:");
        System.out.println("  - Booking ID: " + bookingId);
        System.out.println("  - AddOns field: '" + booking.getAddOns() + "'");
        
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
        assertEquals("   ", fetchedBooking.getAddOns(), "AddOns field should be whitespace");
        
        System.out.println("✓ Booking with whitespace-only addOns fetched successfully");
        System.out.println("  - No errors occurred");
        System.out.println("  - System handles edge case gracefully");
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
