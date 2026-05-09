package edu.cit.macansantos.cleanit.features.auth

data class LoginRequest(
    val email: String,
    val password: String
)