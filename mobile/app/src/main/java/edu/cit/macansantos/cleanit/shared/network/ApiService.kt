package edu.cit.macansantos.cleanit.shared.network

import edu.cit.macansantos.cleanit.features.dashboard.*

import edu.cit.macansantos.cleanit.features.catalog.*

import edu.cit.macansantos.cleanit.features.booking.*

import edu.cit.macansantos.cleanit.features.auth.*

import edu.cit.macansantos.cleanit.features.catalog.AddOn
import edu.cit.macansantos.cleanit.features.dashboard.AdminDashboardStatistics
import edu.cit.macansantos.cleanit.features.dashboard.AvailabilityResponse
import edu.cit.macansantos.cleanit.features.booking.Booking
import edu.cit.macansantos.cleanit.features.booking.CreateBookingRequest
import edu.cit.macansantos.cleanit.features.dashboard.DashboardBooking
import edu.cit.macansantos.cleanit.features.auth.LoginRequest
import edu.cit.macansantos.cleanit.features.auth.LoginResponse
import edu.cit.macansantos.cleanit.features.auth.RegisterRequest
import edu.cit.macansantos.cleanit.features.booking.RescheduleBookingRequest
import edu.cit.macansantos.cleanit.features.catalog.Service
import edu.cit.macansantos.cleanit.features.catalog.Technician
import edu.cit.macansantos.cleanit.features.dashboard.TechnicianAvailabilityRequest
import edu.cit.macansantos.cleanit.features.dashboard.TechnicianBookingActionRequest
import edu.cit.macansantos.cleanit.features.dashboard.TechnicianBookingPhoto
import edu.cit.macansantos.cleanit.features.dashboard.TechnicianChecklistItem
import edu.cit.macansantos.cleanit.features.dashboard.TechnicianStatistics
import edu.cit.macansantos.cleanit.features.dashboard.TechnicianStatusUpdateRequest
import edu.cit.macansantos.cleanit.features.dashboard.AdminBookingsResponse
import edu.cit.macansantos.cleanit.features.dashboard.ChecklistValidationResponse
import edu.cit.macansantos.cleanit.features.dashboard.PhotoValidationResponse
import edu.cit.macansantos.cleanit.features.dashboard.VerificationUser
import edu.cit.macansantos.cleanit.features.users.UserProfile
import retrofit2.Response
import retrofit2.http.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody

interface ApiService {
    @POST("v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ResponseBody>

    @POST("v1/auth/google")
    suspend fun googleAuth(@Body request: Map<String, String>): Response<LoginResponse>

    @POST("v1/auth/oauth-check")
    suspend fun oauthCheck(@Body body: Map<String, String>): Response<edu.cit.macansantos.cleanit.features.auth.OAuthCheckResponse>

    @POST("v1/auth/oauth-complete")
    suspend fun oauthComplete(@Body request: edu.cit.macansantos.cleanit.features.auth.OAuthCompleteRequest): Response<LoginResponse>

    @POST("v1/auth/forgot-password")
    suspend fun forgotPassword(@Body body: Map<String, String>): Response<edu.cit.macansantos.cleanit.features.auth.ForgotPasswordResponse>

    @POST("v1/auth/reset-password")
    suspend fun resetPassword(@Body body: Map<String, String>): Response<ResponseBody>

    @POST("v1/user/change-password")
    suspend fun changePassword(@Body body: Map<String, String>): Response<ResponseBody>

    @GET("v1/services")
    suspend fun getServices(): Response<List<Service>>

    @GET("v1/services/{id}")
    suspend fun getServiceById(@Path("id") id: String): Response<Service>

    @GET("v1/services/{id}/addons")
    suspend fun getServiceAddOns(@Path("id") id: String): Response<List<AddOn>>

    @GET("v1/user/profile/{email}")
    suspend fun getUserProfile(@Path("email") email: String): Response<UserProfile>

    @GET("v1/user/technicians/verified")
    suspend fun getVerifiedTechnicians(): Response<List<Technician>>

    @GET("v1/bookings/client/{clientId}")
    suspend fun getClientBookings(@Path("clientId") clientId: String): Response<List<Booking>>

    @GET("v1/bookings/{id}")
    suspend fun getBookingById(
        @Path("id") id: String,
        @Query("requestingUserId") requestingUserId: String
    ): Response<Booking>

    @POST("v1/bookings/create")
    suspend fun createBooking(@Body booking: CreateBookingRequest): Response<Booking>

    @POST("v1/bookings/{bookingId}/cancel")
    suspend fun cancelBooking(
        @Path("bookingId") bookingId: String,
        @Body request: Map<String, String>
    ): Response<ResponseBody>

    @POST("v1/bookings/{bookingId}/reschedule")
    suspend fun rescheduleBooking(
        @Path("bookingId") bookingId: String,
        @Body request: RescheduleBookingRequest
    ): Response<ResponseBody>

    @GET("v1/technician/{technicianId}/statistics")
    suspend fun getTechnicianStatistics(@Path("technicianId") technicianId: String): Response<TechnicianStatistics>

    @GET("v1/technician/{technicianId}/availability")
    suspend fun getTechnicianAvailability(@Path("technicianId") technicianId: String): Response<AvailabilityResponse>

    @POST("v1/technician/{technicianId}/availability")
    suspend fun setTechnicianAvailability(
        @Path("technicianId") technicianId: String,
        @Body request: TechnicianAvailabilityRequest
    ): Response<AvailabilityResponse>

    @GET("v1/technician/{technicianId}/bookings")
    suspend fun getTechnicianBookings(@Path("technicianId") technicianId: String): Response<List<DashboardBooking>>

    @GET("v1/technician/bookings/pending")
    suspend fun getPendingTechnicianBookings(@Query("technicianId") technicianId: String): Response<List<DashboardBooking>>

    @POST("v1/technician/bookings/{bookingId}/accept")
    suspend fun acceptTechnicianBooking(
        @Path("bookingId") bookingId: String,
        @Body request: TechnicianBookingActionRequest
    ): Response<ResponseBody>

    @POST("v1/technician/bookings/{bookingId}/status")
    suspend fun updateTechnicianBookingStatus(
        @Path("bookingId") bookingId: String,
        @Body request: TechnicianStatusUpdateRequest
    ): Response<ResponseBody>

    @GET("v1/technician/bookings/{bookingId}/checklist")
    suspend fun getTechnicianChecklist(@Path("bookingId") bookingId: String): Response<List<TechnicianChecklistItem>>

    @POST("v1/technician/bookings/{bookingId}/checklist/{checklistItemId}")
    suspend fun toggleTechnicianChecklistItem(
        @Path("bookingId") bookingId: String,
        @Path("checklistItemId") checklistItemId: String,
        @Body request: TechnicianBookingActionRequest
    ): Response<ResponseBody>

    @GET("v1/technician/bookings/{bookingId}/photos")
    suspend fun getTechnicianPhotos(@Path("bookingId") bookingId: String): Response<List<TechnicianBookingPhoto>>

    @Multipart
    @POST("v1/technician/bookings/{bookingId}/photos")
    suspend fun uploadTechnicianPhoto(
        @Path("bookingId") bookingId: String,
        @Part file: MultipartBody.Part,
        @Part("type") type: RequestBody,
        @Part("technicianId") technicianId: RequestBody
    ): Response<ResponseBody>

    @GET("v1/technician/bookings/{bookingId}/validate-checklist")
    suspend fun validateChecklist(@Path("bookingId") bookingId: String): Response<ChecklistValidationResponse>

    @GET("v1/technician/bookings/{bookingId}/validate-photos")
    suspend fun validatePhotos(@Path("bookingId") bookingId: String): Response<PhotoValidationResponse>

    @Multipart
    @POST("v1/auth/upload-image")
    suspend fun uploadImage(@Part file: MultipartBody.Part): Response<Map<String, String>>

    @GET("v1/admin/dashboard/statistics")
    suspend fun getAdminDashboardStatistics(): Response<AdminDashboardStatistics>

    @GET("v1/admin/dashboard/recent-bookings")
    suspend fun getAdminRecentBookings(@Query("limit") limit: Int = 10): Response<List<DashboardBooking>>

    @GET("v1/admin/pending-verifications")
    suspend fun getPendingVerifications(): Response<List<VerificationUser>>

    @POST("v1/admin/verify-user/{userId}")
    suspend fun verifyUser(
        @Path("userId") userId: String,
        @Body request: Map<String, String>
    ): Response<ResponseBody>

    @GET("v1/admin/bookings/by-status")
    suspend fun getAdminBookingsByStatus(
        @Query("statuses") statuses: String
    ): Response<AdminBookingsResponse>

    @POST("v1/admin/bookings/{bookingId}/void")
    suspend fun voidAdminBooking(@Path("bookingId") bookingId: String): Response<ResponseBody>
}
