package com.G4.backend.features.booking;

import com.G4.backend.features.booking.BookingAddon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BookingAddonRepository extends JpaRepository<BookingAddon, BookingAddon.BookingAddonId> {
    List<BookingAddon> findByIdBookingId(UUID bookingId);
    void deleteByIdBookingId(UUID bookingId);
}
