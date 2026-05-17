package edu.cit.macansantos.cleanit.features.users

data class UserProfile(
    val id: String?,
    val name: String?,
    val email: String?,
    val contactNo: String?,
    val role: String?,
    val verified: Boolean?,
    val imageUrl: String?,
    val createdAt: String?
)
