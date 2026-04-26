package com.G4.backend;

import com.G4.backend.entity.Booking;
import com.G4.backend.entity.User;
import com.G4.backend.enums.BookingStatus;
import com.G4.backend.repository.*;
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
        mockClient = createMockUser("client", true);
        mockTechnician = createMockUser("technician", true);
        mockAdmin = createMockUser("admin", true);

        // Setup mock responses
        when(userRepository.findById(mockClient.getId())).thenReturn(java.util.Optional.of(mockClient));
        when(userRepository.findById(mockTechnician.getId())).thenReturn(java.util.Optional.of(mockTechnician));
        when(userRepository.findById(mockAdmin.getId())).thenReturn(java.util.Optional.of(mockAdmin));
        
        // Mock checklist repository for validation
        when(bookingChecklistRepository.countTotalByBookingId(any())).thenReturn(10L);
        when(bookingChecklistRepository.countCheckedByBookingId(any())).thenReturn(10L);
        
        // Mock photo repository for validation
        when(bookingPhotoRepository.countByBookingIdAndType(any(), any())).thenReturn(2L);
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
        verify(notificationService).notifyTechniciansNewBooking(any(Booking.class));

        // 2. Technician accepts the booking (PENDING -> CONFIRMED)
        when(bookingRepository.findById(mockBooking.getId())).thenReturn(java.util.Optional.of(mockBooking));
        mockBooking.setStatus(BookingStatus.PENDING);
        
        Booking acceptedBooking = bookingService.updateBookingStatus(
            mockBooking.getId(), 
            BookingStatus.CONFIRMED, 
            mockTechnician.getId(), 
            "Technician accepted the booking"
        );
        
        assertEquals(BookingStatus.CONFIRMED, acceptedBooking.getStatus());
        assertNotNull(acceptedBooking.getConfirmedAt());

        // 3. Technician starts the service (CONFIRMED -> IN_PROGRESS)
        mockBooking.setStatus(BookingStatus.CONFIRMED);
        
        Booking inProgressBooking = bookingService.updateBookingStatus(
            mockBooking.getId(), 
            BookingStatus.IN_PROGRESS, 
            mockTechnician.getId(), 
            "Service started"
        );
        
        assertEquals(BookingStatus.IN_PROGRESS, inProgressBooking.getStatus());
        assertNotNull(inProgressBooking.getStartedAt());

        // 4. Technician completes the service (IN_PROGRESS -> COMPLETED)
        mockBooking.setStatus(BookingStatus.IN_PROGRESS);
        
        Booking completedBooking = bookingService.updateBookingStatus(
            mockBooking.getId(), 
            BookingStatus.COMPLETED, 
            mockTechnician.getId(), 
            "Service completed successfully"
        );
        
        assertEquals(BookingStatus.COMPLETED, completedBooking.getStatus());
        assertNotNull(completedBooking.getCompletedAt());

        // Verify notifications were sent for each status change (3 status updates)
        verify(notificationService, times(3)).notifyStatusChange(
            any(Booking.class), 
            any(BookingStatus.class), 
            anyString()
        );
    }

    @Test
    void testBookingCancellationWorkflow() {
        Booking mockBooking = createMockBooking();
        mockBooking.setStatus(BookingStatus.PENDING);
        
        when(bookingRepository.findById(mockBooking.getId())).thenReturn(java.util.Optional.of(mockBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(mockBooking);
        
        // Client cancels pending booking
        Booking cancelledBooking = bookingService.cancelBooking(
            mockBooking.getId(), 
            mockClient.getId(), 
            "Client changed their mind"
        );
        
        assertEquals(BookingStatus.CANCELLED, cancelledBooking.getStatus());
        assertEquals("Client changed their mind", cancelledBooking.getStatusReason());
        assertNotNull(cancelledBooking.getCancelledAt());
        
        verify(notificationService).notifyStatusChange(
            any(Booking.class), 
            eq(BookingStatus.CANCELLED), 
            eq("Client changed their mind")
        );
    }

    @Test
    void testNoShowWorkflow() {
        Booking mockBooking = createMockBooking();
        mockBooking.setStatus(BookingStatus.CONFIRMED);
        mockBooking.setTechnicianId(mockTechnician.getId());
        
        when(bookingRepository.findById(mockBooking.getId())).thenReturn(java.util.Optional.of(mockBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(mockBooking);
        
        // Technician marks as no-show
        Booking noShowBooking = bookingService.updateBookingStatus(
            mockBooking.getId(), 
            BookingStatus.NO_SHOW, 
            mockTechnician.getId(), 
            "Client was not available at scheduled time"
        );
        
        assertEquals(BookingStatus.NO_SHOW, noShowBooking.getStatus());
        assertEquals("Client was not available at scheduled time", noShowBooking.getStatusReason());
        assertNotNull(noShowBooking.getNoShowAt());
        
        verify(notificationService).notifyStatusChange(
            any(Booking.class), 
            eq(BookingStatus.NO_SHOW), 
            eq("Client was not available at scheduled time")
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
        
        // Try to confirm booking as client (should fail - only technicians can confirm)
        assertThrows(RuntimeException.class, () -> {
            bookingService.updateBookingStatus(
                mockBooking.getId(), 
                BookingStatus.CONFIRMED, 
                mockClient.getId(), 
                "Unauthorized confirmation"
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