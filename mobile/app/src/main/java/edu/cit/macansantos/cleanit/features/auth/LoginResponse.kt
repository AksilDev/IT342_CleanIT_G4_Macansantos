package edu.cit.macansantos.cleanit.features.auth

data class LoginResponse(
    val id: String,
    val token: String,
    val name: String,
    val email: String,
    val role: String,
    val contactNo: String?,
    val verified: Boolean?,
    val message: String
)