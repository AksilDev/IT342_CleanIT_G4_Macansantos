package com.G4.backend.repository;

import com.G4.backend.entity.BookingChecklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BookingChecklistRepository extends JpaRepository<BookingChecklist, BookingChecklist.BookingChecklistId> {
    
    List<BookingChecklist> findByIdBookingId(UUID bookingId);
    
    @Query("SELECT COUNT(bc) FROM BookingChecklist bc WHERE bc.id.bookingId = :bookingId AND bc.isChecked = true")
    long countCheckedByBookingId(@Param("bookingId") UUID bookingId);
    
    @Query("SELECT COUNT(bc) FROM BookingChecklist bc WHERE bc.id.bookingId = :bookingId")
    long countTotalByBookingId(@Param("bookingId") UUID bookingId);
}
