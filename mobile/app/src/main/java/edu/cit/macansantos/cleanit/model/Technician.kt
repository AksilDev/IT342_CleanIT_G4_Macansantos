package edu.cit.macansantos.cleanit.model

data class Technician(
    val id: String,
    val name: String,
    val email: String,
    val contactNo: String?,
    val imageUrl: String?,
    val verified: Boolean
)
