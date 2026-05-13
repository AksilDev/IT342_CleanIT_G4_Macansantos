package edu.cit.macansantos.cleanit.shared.navigation

import edu.cit.macansantos.cleanit.features.dashboard.TechnicianDashboardActivity

import edu.cit.macansantos.cleanit.features.dashboard.AdminDashboardActivity

import edu.cit.macansantos.cleanit.features.home.HomeActivity

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences

object RoleNavigator {
    fun saveSession(
        sharedPreferences: SharedPreferences,
        token: String,
        role: String?
    ) {
        sharedPreferences.edit()
            .putString("jwt_token", token)
            .putString("user_role", normalizeRole(role))
            .apply()
    }

    fun navigate(
        activity: Activity,
        name: String?,
        email: String?,
        role: String?,
        userId: String?,
        contactNo: String?,
        verified: Boolean?
    ) {
        val intent = when (normalizeRole(role)) {
            "client" -> Intent(activity, HomeActivity::class.java)
            "technician" -> Intent(activity, TechnicianDashboardActivity::class.java)
            "admin" -> Intent(activity, AdminDashboardActivity::class.java)
            else -> Intent(activity, HomeActivity::class.java)
        }.apply {
            putExtra("name", name)
            putExtra("email", email)
            putExtra("role", normalizeRole(role))
            putExtra("userId", userId)
            putExtra("contact", contactNo ?: "")
            putExtra("verified", verified ?: false)
        }

        activity.startActivity(intent)
        activity.finish()
    }

    private fun normalizeRole(role: String?): String {
        return role?.trim()?.lowercase()?.removePrefix("role_").orEmpty()
    }
}
