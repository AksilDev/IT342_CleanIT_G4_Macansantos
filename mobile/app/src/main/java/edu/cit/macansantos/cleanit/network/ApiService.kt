package edu.cit.macansantos.cleanit.network

import edu.cit.macansantos.cleanit.model.LoginRequest
import edu.cit.macansantos.cleanit.model.LoginResponse
import edu.cit.macansantos.cleanit.model.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import okhttp3.ResponseBody

interface ApiService {
    @POST("v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ResponseBody>
}