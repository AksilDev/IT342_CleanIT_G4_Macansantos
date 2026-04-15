package com.G4.backend.repository;

import com.G4.backend.entity.Booking;
import com.G4.backend.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {
    List<Booking> findByClientIdOrderByCreatedAtDesc(UUID clientId);
    List<Booking> findByTechnicianIdOrderByCreatedAtDesc(UUID technicianId);
    List<Booking> findByStatusOrderByCreatedAtDesc(BookingStatus status);
    List<Booking> findByTechnicianIdAndStatusInOrderByBookingDateAsc(UUID technicianId, List<BookingStatus> statuses);
    List<Booking> findByClientIdAndStatusOrderByCreatedAtDesc(UUID clientId, BookingStatus status);
    
    // Find all pending bookings (no technician assigned yet)
    List<Booking> findByStatusAndTechnicianIdIsNullOrderByCreatedAtAsc(BookingStatus status);
    
    // Count active and upcoming bookings for a technician (workload check)
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.technicianId = :technicianId AND b.status IN :statuses")
    long countByTechnicianIdAndStatusIn(@Param("technicianId") UUID technicianId, @Param("statuses") List<BookingStatus> statuses);
    
    // Check for time slot conflicts
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.technicianId = :technicianId AND b.bookingDate = :bookingDate AND b.timeSlot = :timeSlot AND b.status IN :statuses")
    long countByTechnicianIdAndBookingDateAndTimeSlotAndStatusIn(
        @Param("technicianId") UUID technicianId, 
        @Param("bookingDate") LocalDate bookingDate, 
        @Param("timeSlot") String timeSlot, 
        @Param("statuses") List<BookingStatus> statuses
    );
    
    // Find technician's active bookings
    List<Booking> findByTechnicianIdAndStatusIn(UUID technicianId, List<BookingStatus> statuses);
    
    // Admin dashboard statistics
    long countByStatus(BookingStatus status);
    long countByBookingDate(LocalDate bookingDate);
    long countByBookingDateBetween(LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT SUM(b.totalAmount) FROM Booking b WHERE b.status = :status")
    Double sumTotalAmountByStatus(@Param("status") BookingStatus status);
    
    @Query("SELECT SUM(b.totalAmount) FROM Booking b WHERE b.status = :status AND b.bookingDate BETWEEN :startDate AND :endDate")
    Double sumTotalAmountByStatusAndBookingDateBetween(
        @Param("status") BookingStatus status, 
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate
    );
    
    // Recent bookings for admin dashboard - using Spring Data JPA method naming
    List<Booking> findTop10ByOrderByCreatedAtDesc();
    List<Booking> findTop20ByOrderByCreatedAtDesc();
    
    // All bookings ordered by creation date
    List<Booking> findAllByOrderByCreatedAtDesc();
}
