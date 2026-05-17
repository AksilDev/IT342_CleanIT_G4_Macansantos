package edu.cit.macansantos.cleanit.features.auth

data class OAuthCheckResponse(
    val exists: Boolean?,
    val role: String?,
    val token: String?
)

data class ForgotPasswordResponse(
    val message: String?,
    val resetToken: String?,
    val expiresInMinutes: Int?
)

data class OAuthCompleteRequest(
    val email: String,
    val name: String,
    val role: String,
    val contactNo: String,
    val imageUrl: String,
    val tempToken: String? = null
)
