package edu.cit.macansantos.cleanit.shared.network

import android.content.Context
import android.content.SharedPreferences
import android.os.Build

object ApiConfig {
    private const val PREFS_NAME = "ApiConfigPrefs"
    private const val KEY_BASE_URL = "base_url"
    private const val KEY_USE_CUSTOM_URL = "use_custom_url"
    
    // Default URLs
    private const val EMULATOR_URL = "http://10.0.2.2:8080/api/"
    private const val LOCALHOST_URL = "http://192.168.1.5:8080/api/"  // Your computer's IP
    
    /**
     * Get the current API base URL
     */
    fun getBaseUrl(context: Context): String {
        val prefs = getPrefs(context)
        val useCustom = prefs.getBoolean(KEY_USE_CUSTOM_URL, false)
        
        return if (useCustom) {
            prefs.getString(KEY_BASE_URL, EMULATOR_URL) ?: EMULATOR_URL
        } else {
            // Auto-detect: emulator vs physical device
            if (isEmulator()) {
                EMULATOR_URL
            } else {
                // For physical devices, check if custom URL is set
                val customUrl = prefs.getString(KEY_BASE_URL, null)
                customUrl ?: LOCALHOST_URL
            }
        }
    }
    
    /**
     * Set custom API URL
     */
    fun setCustomUrl(context: Context, url: String) {
        getPrefs(context).edit().apply {
            putString(KEY_BASE_URL, url)
            putBoolean(KEY_USE_CUSTOM_URL, true)
            apply()
        }
    }
    
    /**
     * Reset to auto-detect mode
     */
    fun resetToAutoDetect(context: Context) {
        getPrefs(context).edit().apply {
            putBoolean(KEY_USE_CUSTOM_URL, false)
            apply()
        }
    }
    
    /**
     * Get saved custom URL (if any)
     */
    fun getCustomUrl(context: Context): String? {
        return getPrefs(context).getString(KEY_BASE_URL, null)
    }
    
    /**
     * Check if using custom URL
     */
    fun isUsingCustomUrl(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_USE_CUSTOM_URL, false)
    }
    
    /**
     * Check if custom URL is configured
     */
    fun hasCustomUrl(context: Context): Boolean {
        val url = getPrefs(context).getString(KEY_BASE_URL, null)
        return !url.isNullOrBlank()
    }
    
    /**
     * Detect if running on emulator
     */
    fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk" == Build.PRODUCT)
    }
    
    /**
     * Get device type string for display
     */
    fun getDeviceType(): String {
        return if (isEmulator()) "Emulator" else "Physical Device"
    }
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
