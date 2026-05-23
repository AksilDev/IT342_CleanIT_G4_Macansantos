package edu.cit.macansantos.cleanit.shared.network

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private var sharedPreferences: SharedPreferences? = null
    private var appContext: Context? = null
    private var retrofit: Retrofit? = null
    private var currentBaseUrl: String? = null

    /** Broadcast action sent when a 401 is received and session is cleared. */
    const val ACTION_UNAUTHORIZED = "edu.cit.macansantos.cleanit.UNAUTHORIZED"

    fun init(context: Context) {
        appContext = context.applicationContext
        sharedPreferences = context.getSharedPreferences("CleanITPrefs", Context.MODE_PRIVATE)
    }
    
    /**
     * Get the current base URL from ApiConfig
     */
    private fun getBaseUrl(): String {
        return if (appContext != null) {
            ApiConfig.getBaseUrl(appContext!!)
        } else {
            "http://10.0.2.2:8080/api/" // Fallback
        }
    }
    
    /**
     * Force recreation of Retrofit instance
     * Call this after changing API URL
     */
    fun recreate() {
        retrofit = null
        currentBaseUrl = null
    }

    /** Adds the JWT Bearer token to every outgoing request. */
    private val authInterceptor = Interceptor { chain ->
        val token = sharedPreferences?.getString("jwt_token", null)
        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        chain.proceed(request)
    }

    /**
     * Intercepts 401 Unauthorized responses:
     * - Clears the stored JWT and session data
     * - Sends a local broadcast so the current Activity can redirect to Login
     */
    private val unauthorizedInterceptor = Interceptor { chain ->
        val response = chain.proceed(chain.request())

        if (response.code == 401) {
            // Clear session tokens (keep remembered credentials)
            sharedPreferences?.edit()?.apply {
                val rememberEmail = sharedPreferences?.getString("remember_email", null)
                val rememberPassword = sharedPreferences?.getString("remember_password", null)
                clear()
                if (!rememberEmail.isNullOrBlank() && !rememberPassword.isNullOrBlank()) {
                    putString("remember_email", rememberEmail)
                    putString("remember_password", rememberPassword)
                }
                apply()
            }

            // Broadcast so any active Activity can redirect to LoginActivity
            appContext?.sendBroadcast(Intent(ACTION_UNAUTHORIZED).setPackage(appContext?.packageName))
        }

        response
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(unauthorizedInterceptor)
        .build()

    val instance: ApiService
        get() {
            val baseUrl = getBaseUrl()
            
            // Recreate Retrofit if base URL changed
            if (retrofit == null || currentBaseUrl != baseUrl) {
                currentBaseUrl = baseUrl
                retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
            }
            
            return retrofit!!.create(ApiService::class.java)
        }
}
