package com.G4.backend.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "booking_checklist")
public class BookingChecklist {

    @Embeddable
    public static class BookingChecklistId implements Serializable {
        @Column(name = "booking_id")
        private UUID bookingId;

        @Column(name = "checklist_item_id")
        private UUID checklistItemId;

        public BookingChecklistId() {}

        public BookingChecklistId(UUID bookingId, UUID checklistItemId) {
            this.bookingId = bookingId;
            this.checklistItemId = checklistItemId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BookingChecklistId that = (BookingChecklistId) o;
            return Objects.equals(bookingId, that.bookingId) && Objects.equals(checklistItemId, that.checklistItemId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(bookingId, checklistItemId);
        }
    }

    @EmbeddedId
    private BookingChecklistId id;

    @ManyToOne
    @MapsId("bookingId")
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @ManyToOne
    @MapsId("checklistItemId")
    @JoinColumn(name = "checklist_item_id")
    private ChecklistItem checklistItem;

    @Column(name = "is_checked", nullable = false)
    private Boolean isChecked = false;

    @Column(name = "checked_at")
    private LocalDateTime checkedAt;

    public BookingChecklist() {}

    public BookingChecklist(Booking booking, ChecklistItem checklistItem) {
        this.booking = booking;
        this.checklistItem = checklistItem;
        this.id = new BookingChecklistId(booking.getId(), checklistItem.getId());
        this.isChecked = false;
    }

    // Getters and Setters
    public BookingChecklistId getId() { return id; }
    public void setId(BookingChecklistId id) { this.id = id; }

    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }

    public ChecklistItem getChecklistItem() { return checklistItem; }
    public void setChecklistItem(ChecklistItem checklistItem) { this.checklistItem = checklistItem; }

    public Boolean getIsChecked() { return isChecked; }
    public void setIsChecked(Boolean isChecked) { 
        this.isChecked = isChecked;
        if (isChecked && this.checkedAt == null) {
            this.checkedAt = LocalDateTime.now();
        }
    }

    public LocalDateTime getCheckedAt() { return checkedAt; }
    public void setCheckedAt(LocalDateTime checkedAt) { this.checkedAt = checkedAt; }
}
