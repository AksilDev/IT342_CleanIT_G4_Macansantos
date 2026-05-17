package edu.cit.macansantos.cleanit.features.dashboard

import com.google.gson.annotations.SerializedName

data class DashboardBooking(
    val id: String?,
    val bookingCode: String?,
    val serviceType: String?,
    val deviceType: String?,
    val status: String?,
    val totalAmount: Double?,
    val bookingDate: String?,
    val timeSlot: String?,
    val address: String?,
    val landmark: String?,
    val clientName: String?,
    val clientContact: String?,
    val clientEmail: String?,
    val technicianName: String?
)

data class TechnicianStatistics(
    val totalBookings: Int?,
    val completed: Int?,
    val confirmed: Int?,
    val inProgress: Int?,
    val active: Int?,
    val totalEarnings: Double?
)

data class AvailabilityResponse(
    val isAvailable: Boolean?
)

data class AdminDashboardStatistics(
    val total: Int?,
    val pending: Int?,
    val confirmed: Int?,
    @SerializedName("in_progress")
    val inProgress: Int?,
    val completed: Int?,
    val cancelled: Int?,
    val activeBookings: Int?,
    val confirmedBookings: Int?,
    val totalRevenue: Double?,
    val monthRevenue: Double?
)

data class VerificationUser(
    val id: String?,
    val name: String?,
    val email: String?,
    val contactNo: String?,
    val role: String?,
    val verified: Boolean?
)

data class TechnicianChecklistItem(
    val id: String?,
    val label: String?,
    val isChecked: Boolean?,
    val checkedAt: String?
)

data class TechnicianBookingPhoto(
    val id: String?,
    val type: String?,
    val fileUrl: String?,
    val uploadedAt: String?
)

data class TechnicianAvailabilityRequest(
    val isAvailable: Boolean
)

data class TechnicianBookingActionRequest(
    val technicianId: String
)

data class TechnicianStatusUpdateRequest(
    val status: String,
    val technicianId: String,
    val reason: String
)
