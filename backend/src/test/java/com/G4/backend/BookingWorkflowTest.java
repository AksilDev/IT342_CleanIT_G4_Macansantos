package com.G4.backend;

import com.G4.backend.entity.Booking;
import com.G4.backend.entity.User;
import com.G4.backend.enums.BookingStatus;
import com.G4.backend.repository.BookingRepository;
import com.G4.backend.repository.UserRepository;
import com.G4.backend.service.BookingService;
import com.G4.backend.service.BookingNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class to demonstrate the booking workflow
 * This is a unit test that mocks the dependencies
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
public class BookingWorkflowTest {

    private BookingService bookingService;
    private BookingRepository bookingRepository;
    private UserRepository userRepository;
    private BookingNotificationService notificationService;

    private User mockClient;
    private User mockTechnician;
    private User mockAdmin;

    @BeforeEach
    void setUp() {
        // Mock repositories and services
        bookingRepository = mock(BookingRepository.class);
        userRepository = mock(UserRepository.class);
        notificationService = mock(BookingNotificationService.class);
        
        bookingService = new BookingService(bookingRepository, userRepository, notificationService);

        // Create mock users
        mockClient = createMockUser("client", true);
        mockTechnician = createMockUser("technician", true);
        mockAdmin = createMockUser("admin", true);

        // Setup mock responses
        when(userRepository.findById(mockClient.getId())).thenReturn(java.util.Optional.of(mockClient));
        when(userRepository.findById(mockTechnician.getId())).thenReturn(java.util.Optional.of(mockTechnician));
        when(userRepository.findById(mockAdmin.getId())).thenReturn(java.util.Optional.of(mockAdmin));
    }

    @Test
    void testCompleteBookingWorkflow() {
        // 1. Create a booking
        Map<String, Object> bookingData = createBookingData();
        Booking mockBooking = createMockBooking();
        
        when(bookingRepository.save(any(Booking.class))).thenReturn(mockBooking);
        
        Booking createdBooking = bookingService.createBooking(bookingData);
        
        assertNotNull(createdBooking);
        assertEquals(BookingStatus.PENDING, createdBooking.getStatus());
        verify(notificationService).notifyAdminNewBooking(any(Booking.class));

        // 2. Technician accepts the booking
        when(bookingRepository.findById(mockBooking.getId())).thenReturn(java.util.Optional.of(mockBooking));
        
        Booking confirmedBooking = bookingService.updateBookingStatus(
            mockBooking.getId(), 
            BookingStatus.CONFIRMED, 
            mockTechnician.getId(), 
            "Technician accepted the booking"
        );
        
        assertEquals(BookingStatus.CONFIRMED, confirmedBooking.getStatus());

        // 3. Technician starts the service
        mockBooking.setStatus(BookingStatus.CONFIRMED);
        
        Booking confirmedBooking = bookingService.updateBookingStatus(
            mockBooking.getId(), 
            BookingStatus.CONFIRMED, 
            mockTechnician.getId(), 
            "Confirmed availability"
        );
        
        assertEquals(BookingStatus.CONFIRMED, confirmedBooking.getStatus());
        assertNotNull(confirmedBooking.getConfirmedAt());

        // 4. Technician starts the service
        mockBooking.setStatus(BookingStatus.CONFIRMED);
        
        Booking inProgressBooking = bookingService.updateBookingStatus(
            mockBooking.getId(), 
            BookingStatus.IN_PROGRESS, 
            mockTechnician.getId(), 
            "Service started"
        );
        
        assertEquals(BookingStatus.IN_PROGRESS, inProgressBooking.getStatus());

        // 5. Technician completes the service
        mockBooking.setStatus(BookingStatus.IN_PROGRESS);
        
        Booking completedBooking = bookingService.updateBookingStatus(
            mockBooking.getId(), 
            BookingStatus.COMPLETED, 
            mockTechnician.getId(), 
            "Service completed successfully"
        );
        
        assertEquals(BookingStatus.COMPLETED, completedBooking.getStatus());
        assertNotNull(completedBooking.getCompletedAt());

        // Verify notifications were sent for each status change
        verify(notificationService, times(4)).notifyStatusChange(
            any(Booking.class), 
            any(BookingStatus.class), 
            any(BookingStatus.class), 
            anyString()
        );
    }

    @Test
    void testBookingRejectionWorkflow() {
        Booking mockBooking = createMockBooking();
        mockBooking.setStatus(BookingStatus.PENDING);
        
        when(bookingRepository.findById(mockBooking.getId())).thenReturn(java.util.Optional.of(mockBooking));
        
        // Admin rejects the booking
        Booking rejectedBooking = bookingService.updateBookingStatus(
            mockBooking.getId(), 
            BookingStatus.REJECTED, 
            mockAdmin.getId(), 
            "Technician not available on requested date"
        );
        
        assertEquals(BookingStatus.REJECTED, rejectedBooking.getStatus());
        assertEquals("Technician not available on requested date", rejectedBooking.getStatusReason());
        
        verify(notificationService).notifyStatusChange(
            any(Booking.class), 
            eq(BookingStatus.PENDING), 
            eq(BookingStatus.REJECTED), 
            eq("Technician not available on requested date")
        );
    }

    @Test
    void testBookingRescheduleWorkflow() {
        Booking mockBooking = createMockBooking();
        mockBooking.setStatus(BookingStatus.CONFIRMED);
        
        when(bookingRepository.findById(mockBooking.getId())).thenReturn(java.util.Optional.of(mockBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(mockBooking);
        
        LocalDate newDate = LocalDate.now().plusDays(7);
        String newTimeSlot = "2:00 PM - 4:00 PM";
        
        Booking rescheduledBooking = bookingService.rescheduleBooking(
            mockBooking.getId(), 
            newDate, 
            newTimeSlot, 
            mockClient.getId()
        );
        
        assertEquals(BookingStatus.RESCHEDULED, rescheduledBooking.getStatus());
        assertEquals(newDate, rescheduledBooking.getBookingDate());
        assertEquals(newTimeSlot, rescheduledBooking.getTimeSlot());
        
        verify(notificationService).notifyStatusChange(
            any(Booking.class), 
            eq(BookingStatus.CONFIRMED), 
            eq(BookingStatus.RESCHEDULED), 
            contains("Rescheduled to")
        );
    }

    @Test
    void testInvalidStatusTransition() {
        Booking mockBooking = createMockBooking();
        mockBooking.setStatus(BookingStatus.COMPLETED);
        
        when(bookingRepository.findById(mockBooking.getId())).thenReturn(java.util.Optional.of(mockBooking));
        
        // Try to change completed booking to pending (invalid transition)
        assertThrows(RuntimeException.class, () -> {
            bookingService.updateBookingStatus(
                mockBooking.getId(), 
                BookingStatus.PENDING, 
                mockAdmin.getId(), 
                "Invalid transition"
            );
        });
    }

    @Test
    void testUnauthorizedStatusChange() {
        Booking mockBooking = createMockBooking();
        mockBooking.setStatus(BookingStatus.PENDING);
        
        when(bookingRepository.findById(mockBooking.getId())).thenReturn(java.util.Optional.of(mockBooking));
        
        // Try to approve booking as client (should fail)
        assertThrows(RuntimeException.class, () -> {
            bookingService.updateBookingStatus(
                mockBooking.getId(), 
                BookingStatus.APPROVED, 
                mockClient.getId(), 
                "Unauthorized approval"
            );
        });
    }

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
        booking.setTechnicianId(mockTechnician.getId());
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
        data.put("technicianId", mockTechnician.getId().toString());
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