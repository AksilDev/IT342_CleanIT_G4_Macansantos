package edu.cit.macansantos.cleanit.features.catalog

data class Service(
    val id: String,
    val name: String,
    val description: String,
    val basePrice: Double,
    val durationMinutes: Int,
    val isActive: Boolean
)
