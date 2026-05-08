package edu.cit.macansantos.cleanit.model

data class Service(
    val id: String,
    val name: String,
    val description: String,
    val basePrice: Double,
    val durationMinutes: Int,
    val isActive: Boolean
)

data class Booking(
    val id: String,
    val bookingCode: String,
    val clientId: String,
    val technicianId: String?,
    val serviceType: String,
    val deviceType: String,
    val status: String,
    val totalAmount: Double,
    val bookingDate: String,
    val timeSlot: String,
    val address: String,
    val landmark: String?,
    val specialInstructions: String?,
    val createdAt: String,
    val clientName: String?,
    val technicianName: String?,
    val addOns: List<String>?,
    val photos: List<BookingPhoto>?,
    val statusReason: String?
)

data class BookingPhoto(
    val id: String,
    val type: String, // "BEFORE" or "AFTER"
    val fileUrl: String,
    val uploadedAt: String
)
