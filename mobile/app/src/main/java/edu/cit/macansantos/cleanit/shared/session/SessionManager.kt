package edu.cit.macansantos.cleanit.shared.session

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import edu.cit.macansantos.cleanit.features.auth.LoginResponse
import edu.cit.macansantos.cleanit.features.users.UserProfile
import edu.cit.macansantos.cleanit.shared.navigation.RoleNavigator

object SessionManager {
    const val PREFS_NAME = "CleanITPrefs"
    private const val KEY_TOKEN = "jwt_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_ROLE = "user_role"
    private const val KEY_USER_CONTACT = "user_contact"
    private const val KEY_USER_VERIFIED = "user_verified"

    fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun hasValidSession(prefs: SharedPreferences): Boolean {
        return !prefs.getString(KEY_TOKEN, null).isNullOrBlank() &&
            !prefs.getString(KEY_USER_ID, null).isNullOrBlank() &&
            !prefs.getString(KEY_USER_EMAIL, null).isNullOrBlank()
    }

    fun saveSession(prefs: SharedPreferences, token: String, user: LoginResponse) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USER_ID, user.id)
            .putString(KEY_USER_NAME, user.name)
            .putString(KEY_USER_EMAIL, user.email)
            .putString(KEY_USER_ROLE, normalizeRole(user.role))
            .putString(KEY_USER_CONTACT, user.contactNo.orEmpty())
            .putBoolean(KEY_USER_VERIFIED, user.verified == true)
            .apply()
    }

    fun saveUserProfile(prefs: SharedPreferences, profile: UserProfile) {
        prefs.edit()
            .putString(KEY_USER_ID, profile.id.orEmpty())
            .putString(KEY_USER_NAME, profile.name.orEmpty())
            .putString(KEY_USER_EMAIL, profile.email.orEmpty())
            .putString(KEY_USER_ROLE, normalizeRole(profile.role))
            .putString(KEY_USER_CONTACT, profile.contactNo.orEmpty())
            .putBoolean(KEY_USER_VERIFIED, profile.verified == true)
            .apply()
    }

    fun clearSession(prefs: SharedPreferences) {
        val rememberEmail = prefs.getString("remember_email", null)
        val rememberPassword = prefs.getString("remember_password", null)
        prefs.edit().clear().apply()
        if (!rememberEmail.isNullOrBlank() && !rememberPassword.isNullOrBlank()) {
            prefs.edit()
                .putString("remember_email", rememberEmail)
                .putString("remember_password", rememberPassword)
                .apply()
        }
    }

    fun navigateFromSession(activity: Activity, prefs: SharedPreferences) {
        RoleNavigator.navigate(
            activity = activity,
            name = prefs.getString(KEY_USER_NAME, "User"),
            email = prefs.getString(KEY_USER_EMAIL, ""),
            role = prefs.getString(KEY_USER_ROLE, "client"),
            userId = prefs.getString(KEY_USER_ID, ""),
            contactNo = prefs.getString(KEY_USER_CONTACT, ""),
            verified = prefs.getBoolean(KEY_USER_VERIFIED, false)
        )
    }

    fun getEmail(prefs: SharedPreferences): String =
        prefs.getString(KEY_USER_EMAIL, "").orEmpty()

    fun saveToken(prefs: SharedPreferences, token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    private fun normalizeRole(role: String?): String =
        role?.trim()?.lowercase()?.removePrefix("role_").orEmpty()
}
