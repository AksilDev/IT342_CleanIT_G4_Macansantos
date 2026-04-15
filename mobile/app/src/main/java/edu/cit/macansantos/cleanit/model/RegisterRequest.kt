package edu.cit.macansantos.cleanit.model

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val role: String = "client",
    val contactNo: String = "N/A"
)