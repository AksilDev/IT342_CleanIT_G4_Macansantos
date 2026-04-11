package com.G4.backend.controller;

import com.G4.backend.entity.Booking;
import com.G4.backend.entity.User;
import com.G4.backend.repository.BookingRepository;
import com.G4.backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/bookings")
@CrossOrigin(origins = "http://localhost:5173")
public class BookingController {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    public BookingController(BookingRepository bookingRepository, UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createBooking(@RequestBody Map<String, Object> request) {
        try {
            Booking booking = new Booking();
            booking.setClientId(UUID.fromString((String) request.get("clientId")));
            booking.setTechnicianId(UUID.fromString((String) request.get("technicianId")));
            booking.setServiceType((String) request.get("serviceType"));
            booking.setDeviceType((String) request.get("deviceType"));
            
            List<String> addOns = (List<String>) request.get("addOns");
            if (addOns != null && !addOns.isEmpty()) {
                booking.setAddOns(String.join(",", addOns));
            }
            
            booking.setTimeSlot((String) request.get("timeSlot"));
            booking.setBookingDate(LocalDate.parse((String) request.get("bookingDate")));
            booking.setAddress((String) request.get("address"));
            booking.setLandmark((String) request.get("landmark"));
            booking.setSpecialInstructions((String) request.get("specialInstructions"));
            booking.setTotalAmount(Double.valueOf(request.get("totalAmount").toString()));
            booking.setStatus("pending");
            booking.setCreatedAt(LocalDateTime.now());

            Booking saved = bookingRepository.save(booking);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to create booking: " + e.getMessage());
        }
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<?> getClientBookings(@PathVariable UUID clientId) {
        try {
            List<Booking> bookings = bookingRepository.findByClientIdOrderByCreatedAtDesc(clientId);
            List<Map<String, Object>> response = new ArrayList<>();
            
            for (Booking booking : bookings) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", booking.getId());
                map.put("serviceType", booking.getServiceType());
                map.put("deviceType", booking.getDeviceType());
                map.put("addOns", booking.getAddOns() != null ? Arrays.asList(booking.getAddOns().split(",")) : new ArrayList<>());
                map.put("timeSlot", booking.getTimeSlot());
                map.put("bookingDate", booking.getBookingDate());
                map.put("address", booking.getAddress());
                map.put("landmark", booking.getLandmark());
                map.put("specialInstructions", booking.getSpecialInstructions());
                map.put("totalAmount", booking.getTotalAmount());
                map.put("status", booking.getStatus());
                map.put("createdAt", booking.getCreatedAt());
                
                User technician = userRepository.findById(booking.getTechnicianId()).orElse(null);
                if (technician != null) {
                    map.put("technicianName", technician.getName());
                    map.put("technicianContact", technician.getContactNo());
                }
                
                response.add(map);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to fetch bookings: " + e.getMessage());
        }
    }

    @GetMapping("/technician/{technicianId}")
    public ResponseEntity<?> getTechnicianBookings(@PathVariable UUID technicianId) {
        try {
            List<Booking> bookings = bookingRepository.findByTechnicianIdOrderByCreatedAtDesc(technicianId);
            List<Map<String, Object>> response = new ArrayList<>();
            
            for (Booking booking : bookings) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", booking.getId());
                map.put("serviceType", booking.getServiceType());
                map.put("deviceType", booking.getDeviceType());
                map.put("timeSlot", booking.getTimeSlot());
                map.put("bookingDate", booking.getBookingDate());
                map.put("address", booking.getAddress());
                map.put("status", booking.getStatus());
                map.put("totalAmount", booking.getTotalAmount());
                
                User client = userRepository.findById(booking.getClientId()).orElse(null);
                if (client != null) {
                    map.put("clientName", client.getName());
                    map.put("clientContact", client.getContactNo());
                }
                
                response.add(map);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to fetch bookings: " + e.getMessage());
        }
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<?> getBookingDetails(@PathVariable UUID bookingId) {
        try {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found"));
            
            Map<String, Object> map = new HashMap<>();
            map.put("id", booking.getId());
            map.put("serviceType", booking.getServiceType());
            map.put("deviceType", booking.getDeviceType());
            map.put("addOns", booking.getAddOns() != null ? Arrays.asList(booking.getAddOns().split(",")) : new ArrayList<>());
            map.put("timeSlot", booking.getTimeSlot());
            map.put("bookingDate", booking.getBookingDate());
            map.put("address", booking.getAddress());
            map.put("landmark", booking.getLandmark());
            map.put("specialInstructions", booking.getSpecialInstructions());
            map.put("totalAmount", booking.getTotalAmount());
            map.put("status", booking.getStatus());
            map.put("createdAt", booking.getCreatedAt());
            
            User technician = userRepository.findById(booking.getTechnicianId()).orElse(null);
            if (technician != null) {
                map.put("technicianName", technician.getName());
                map.put("technicianContact", technician.getContactNo());
                map.put("technicianEmail", technician.getEmail());
            }
            
            User client = userRepository.findById(booking.getClientId()).orElse(null);
            if (client != null) {
                map.put("clientName", client.getName());
                map.put("clientContact", client.getContactNo());
            }
            
            return ResponseEntity.ok(map);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to fetch booking: " + e.getMessage());
        }
    }

    @PostMapping("/{bookingId}/status")
    public ResponseEntity<?> updateBookingStatus(@PathVariable UUID bookingId, @RequestBody Map<String, String> request) {
        try {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found"));
            
            String newStatus = request.get("status");
            if (newStatus == null || !Arrays.asList("pending", "confirmed", "in_progress", "completed", "cancelled").contains(newStatus)) {
                return ResponseEntity.badRequest().body("Invalid status");
            }
            
            booking.setStatus(newStatus);
            bookingRepository.save(booking);
            return ResponseEntity.ok("Status updated successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to update status: " + e.getMessage());
        }
    }
}
