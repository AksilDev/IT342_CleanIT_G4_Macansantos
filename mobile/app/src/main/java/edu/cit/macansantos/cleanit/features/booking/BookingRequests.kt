package edu.cit.macansantos.cleanit.features.booking

data class CreateBookingRequest(
    val clientId: String,
    val technicianId: String,
    val serviceId: String,
    val serviceType: String,
    val deviceType: String,
    val addOns: List<String>,
    val timeSlot: String,
    val bookingDate: String,
    val address: String,
    val landmark: String,
    val specialInstructions: String,
    val totalAmount: Double
)

data class RescheduleBookingRequest(
    val requestedBy: String,
    val newBookingDate: String,
    val newTimeSlot: String,
    val reason: String
)

data class Booking(
    val id: String?,
    val bookingCode: String?,
    val clientId: String?,
    val technicianId: String?,
    val serviceType: String?,
    val deviceType: String?,
    val status: String?,
    val totalAmount: Double?,
    val bookingDate: String?,
    val timeSlot: String?,
    val address: String?,
    val landmark: String?,
    val specialInstructions: String?,
    val createdAt: String?,
    val clientName: String?,
    val technicianName: String?,
    val addOns: List<String>?,
    val photos: List<BookingPhoto>?,
    val statusReason: String?
)

data class BookingPhoto(
    val id: String?,
    val type: String?,
    val fileUrl: String?,
    val uploadedAt: String?
)
