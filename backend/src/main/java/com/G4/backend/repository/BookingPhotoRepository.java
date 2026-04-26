package com.G4.backend.repository;

import com.G4.backend.entity.BookingPhoto;
import com.G4.backend.enums.PhotoType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BookingPhotoRepository extends JpaRepository<BookingPhoto, UUID> {
    
    List<BookingPhoto> findByBookingId(UUID bookingId);
    
    List<BookingPhoto> findByBookingIdAndType(UUID bookingId, PhotoType type);
    
    @Query("SELECT COUNT(bp) FROM BookingPhoto bp WHERE bp.bookingId = :bookingId AND bp.type = :type")
    long countByBookingIdAndType(@Param("bookingId") UUID bookingId, @Param("type") PhotoType type);
}
