package com.G4.backend.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "booking_addons")
public class BookingAddon {

    @Embeddable
    public static class BookingAddonId implements Serializable {
        @Column(name = "booking_id")
        private UUID bookingId;

        @Column(name = "addon_id")
        private UUID addonId;

        public BookingAddonId() {}

        public BookingAddonId(UUID bookingId, UUID addonId) {
            this.bookingId = bookingId;
            this.addonId = addonId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BookingAddonId that = (BookingAddonId) o;
            return Objects.equals(bookingId, that.bookingId) && Objects.equals(addonId, that.addonId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(bookingId, addonId);
        }
    }

    @EmbeddedId
    private BookingAddonId id;

    @ManyToOne
    @MapsId("bookingId")
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @ManyToOne
    @MapsId("addonId")
    @JoinColumn(name = "addon_id")
    private AddOn addOn;

    @Column(name = "price_at_booking", nullable = false)
    private Double priceAtBooking;

    public BookingAddon() {}

    public BookingAddon(Booking booking, AddOn addOn, Double priceAtBooking) {
        this.booking = booking;
        this.addOn = addOn;
        this.priceAtBooking = priceAtBooking;
        this.id = new BookingAddonId(booking.getId(), addOn.getId());
    }

    // Getters and Setters
    public BookingAddonId getId() { return id; }
    public void setId(BookingAddonId id) { this.id = id; }

    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }

    public AddOn getAddOn() { return addOn; }
    public void setAddOn(AddOn addOn) { this.addOn = addOn; }

    public Double getPriceAtBooking() { return priceAtBooking; }
    public void setPriceAtBooking(Double priceAtBooking) { this.priceAtBooking = priceAtBooking; }
}
