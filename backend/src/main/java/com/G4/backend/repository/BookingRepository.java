package com.G4.backend.repository;

import com.G4.backend.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {
    List<Booking> findByClientIdOrderByCreatedAtDesc(UUID clientId);
    List<Booking> findByTechnicianIdOrderByCreatedAtDesc(UUID technicianId);
    List<Booking> findByStatusOrderByCreatedAtDesc(String status);
}
