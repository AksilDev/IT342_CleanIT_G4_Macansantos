package edu.cit.macansantos.cleanit.features.dashboard

data class AdminBookingsResponse(
    val bookings: List<AdminBookingDetail>?,
    val count: Int?,
    val statuses: List<String>?
)

data class AdminBookingDetail(
    val id: String?,
    val bookingCode: String?,
    val clientName: String?,
    val clientEmail: String?,
    val clientContact: String?,
    val technicianName: String?,
    val technicianEmail: String?,
    val technicianContact: String?,
    val serviceType: String?,
    val deviceType: String?,
    val addOns: List<String>?,
    val totalAmount: Double?,
    val bookingDate: String?,
    val timeSlot: String?,
    val address: String?,
    val landmark: String?,
    val specialInstructions: String?,
    val status: String?,
    val createdAt: String?
)

data class ChecklistValidationResponse(
    val isComplete: Boolean?,
    val incompleteItems: List<String>?
)

data class PhotoValidationResponse(
    val hasRequiredPhotos: Boolean?,
    val missingRequirements: List<String>?
)
