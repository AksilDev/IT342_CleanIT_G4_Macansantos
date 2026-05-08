package edu.cit.macansantos.cleanit.network

import edu.cit.macansantos.cleanit.model.AddOn
import edu.cit.macansantos.cleanit.model.Booking
import edu.cit.macansantos.cleanit.model.LoginRequest
import edu.cit.macansantos.cleanit.model.LoginResponse
import edu.cit.macansantos.cleanit.model.RegisterRequest
import edu.cit.macansantos.cleanit.model.Service
import edu.cit.macansantos.cleanit.model.Technician
import retrofit2.Response
import retrofit2.http.*
import okhttp3.ResponseBody

interface ApiService {
    @POST("v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ResponseBody>

    @GET("v1/services")
    suspend fun getServices(): Response<List<Service>>

    @GET("v1/services/{id}")
    suspend fun getServiceById(@Path("id") id: String): Response<Service>

    @GET("v1/services/{id}/addons")
    suspend fun getServiceAddOns(@Path("id") id: String): Response<List<AddOn>>

    @GET("v1/user/technicians/verified")
    suspend fun getVerifiedTechnicians(): Response<List<Technician>>

    @GET("v1/bookings/client/{clientId}")
    suspend fun getClientBookings(@Path("clientId") clientId: String): Response<List<Booking>>

    @GET("v1/bookings/{id}")
    suspend fun getBookingById(@Path("id") id: String): Response<Booking>

    @POST("v1/bookings/create")
    suspend fun createBooking(@Body booking: Map<String, Any>): Response<Booking>

    @POST("v1/bookings/{bookingId}/cancel")
    suspend fun cancelBooking(
        @Path("bookingId") bookingId: String,
        @Body request: Map<String, String>
    ): Response<ResponseBody>

    @POST("v1/bookings/{bookingId}/reschedule")
    suspend fun rescheduleBooking(
        @Path("bookingId") bookingId: String,
        @Body request: Map<String, Any>
    ): Response<ResponseBody>
}