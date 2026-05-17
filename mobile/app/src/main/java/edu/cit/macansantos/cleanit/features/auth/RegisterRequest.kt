package edu.cit.macansantos.cleanit.features.auth

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val role: String = "client",
    val contactNo: String = "N/A",
    val imageUrl: String? = null
)