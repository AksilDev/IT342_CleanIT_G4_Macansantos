package edu.cit.macansantos.cleanit.model

data class LoginResponse(
    val token: String,
    val name: String,
    val email: String,
    val role: String,
    val message: String
)