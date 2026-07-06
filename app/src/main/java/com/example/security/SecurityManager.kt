package com.example.security

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest

object SecurityManager {
    private const val PREFS_NAME = "luf_vault_secure_prefs"
    private const val KEY_SETUP_COMPLETE = "setup_complete"
    private const val KEY_PASSWORD_HASH = "password_hash"
    private const val KEY_FINGERPRINT_HASH = "fingerprint_hash"
    private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    private const val KEY_LOCK_TIMEOUT_MINS = "lock_timeout_mins"
    private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
    private const val KEY_LOCKOUT_TIMESTAMP = "lockout_timestamp"
    private const val KEY_PREVENT_SCREENSHOTS = "prevent_screenshots"
    private const val KEY_PREVENT_RECORDINGS = "prevent_recordings"
    private const val KEY_KEY_FILE_NAME = "key_file_name"
    private const val KEY_THEME = "theme" // "system", "light", "dark"
    private const val KEY_AUTO_LOCK_TIMEOUT = "auto_lock_timeout" // in minutes
    private const val KEY_LANGUAGE = "language" // "fa", "en"
    private const val KEY_CUSTOM_APP_NAME = "custom_app_name"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun hashString(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun hashBytes(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(bytes)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun isSetupCompleted(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SETUP_COMPLETE, false)
    }

    fun setSetupCompleted(context: Context, completed: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SETUP_COMPLETE, completed).apply()
    }

    fun savePassword(context: Context, password: String) {
        val hashed = hashString(password)
        getPrefs(context).edit().putString(KEY_PASSWORD_HASH, hashed).apply()
    }

    fun verifyPassword(context: Context, password: String): Boolean {
        val stored = getPrefs(context).getString(KEY_PASSWORD_HASH, null) ?: return false
        return stored == hashString(password)
    }

    fun saveKeyFileFingerprint(context: Context, hash: String) {
        getPrefs(context).edit().putString(KEY_FINGERPRINT_HASH, hash).apply()
    }

    fun verifyKeyFileFingerprint(context: Context, hash: String): Boolean {
        val stored = getPrefs(context).getString(KEY_FINGERPRINT_HASH, null) ?: return false
        return stored == hash
    }

    fun getBiometricEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun getLockTimeoutMinutes(context: Context): Int {
        return getPrefs(context).getInt(KEY_LOCK_TIMEOUT_MINS, 5) // default 5 minutes
    }

    fun setLockTimeoutMinutes(context: Context, minutes: Int) {
        getPrefs(context).edit().putInt(KEY_LOCK_TIMEOUT_MINS, minutes).apply()
    }

    fun getPreventScreenshots(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_PREVENT_SCREENSHOTS, false)
    }

    fun setPreventScreenshots(context: Context, prevent: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_PREVENT_SCREENSHOTS, prevent).apply()
    }

    fun getPreventRecordings(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_PREVENT_RECORDINGS, false)
    }

    fun setPreventRecordings(context: Context, prevent: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_PREVENT_RECORDINGS, prevent).apply()
    }

    fun getKeyFileName(context: Context): String {
        return getPrefs(context).getString(KEY_KEY_FILE_NAME, "") ?: ""
    }

    fun setKeyFileName(context: Context, name: String) {
        getPrefs(context).edit().putString(KEY_KEY_FILE_NAME, name).apply()
    }

    fun getTheme(context: Context): String {
        return getPrefs(context).getString(KEY_THEME, "system") ?: "system"
    }

    fun setTheme(context: Context, theme: String) {
        getPrefs(context).edit().putString(KEY_THEME, theme).apply()
    }

    fun getAutoLockTimeoutMinutes(context: Context): Int {
        return getPrefs(context).getInt(KEY_AUTO_LOCK_TIMEOUT, 10) // default 10 minutes
    }

    fun setAutoLockTimeoutMinutes(context: Context, minutes: Int) {
        getPrefs(context).edit().putInt(KEY_AUTO_LOCK_TIMEOUT, minutes).apply()
    }

    // Locking out mechanics (Lock after 5 wrong passwords)
    fun getFailedAttempts(context: Context): Int {
        return getPrefs(context).getInt(KEY_FAILED_ATTEMPTS, 0)
    }

    fun recordFailedAttempt(context: Context): Int {
        val prefs = getPrefs(context)
        val current = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
        val edit = prefs.edit().putInt(KEY_FAILED_ATTEMPTS, current)
        if (current >= 5) {
            edit.putLong(KEY_LOCKOUT_TIMESTAMP, System.currentTimeMillis())
        }
        edit.apply()
        return current
    }

    fun resetFailedAttempts(context: Context) {
        getPrefs(context).edit()
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_TIMESTAMP, 0L)
            .apply()
    }

    fun getRemainingLockTime(context: Context): Long {
        val lockoutTime = getPrefs(context).getLong(KEY_LOCKOUT_TIMESTAMP, 0L)
        if (lockoutTime == 0L) return 0L

        val timeoutMinutes = getLockTimeoutMinutes(context)
        val elapsed = System.currentTimeMillis() - lockoutTime
        val durationMillis = timeoutMinutes * 60 * 1000L
        val remaining = durationMillis - elapsed

        return if (remaining > 0L) remaining else 0L
    }

    fun isLockedOut(context: Context): Boolean {
        return getRemainingLockTime(context) > 0L
    }

    fun getLanguage(context: Context): String {
        return getPrefs(context).getString(KEY_LANGUAGE, "fa") ?: "fa"
    }

    fun setLanguage(context: Context, language: String) {
        getPrefs(context).edit().putString(KEY_LANGUAGE, language).apply()
    }

    fun getCustomAppName(context: Context): String {
        return getPrefs(context).getString(KEY_CUSTOM_APP_NAME, "صندوقچه LUF") ?: "صندوقچه LUF"
    }

    fun setCustomAppName(context: Context, name: String) {
        getPrefs(context).edit().putString(KEY_CUSTOM_APP_NAME, name).apply()
    }

    fun getSelectedAlias(context: Context): String {
        return getPrefs(context).getString("selected_alias", "com.example.MainActivityDefault") ?: "com.example.MainActivityDefault"
    }

    fun setSelectedAlias(context: Context, alias: String) {
        getPrefs(context).edit().putString("selected_alias", alias).apply()
    }

    fun applySelectedAlias(context: Context, aliasName: String) {
        val pm = context.packageManager
        val packageName = context.packageName

        val aliases = listOf(
            "com.example.MainActivityDefault",
            "com.example.MainActivityCalculator",
            "com.example.MainActivityWeather",
            "com.example.MainActivityNotes"
        )

        aliases.forEach { alias ->
            val enableState = if (alias == aliasName) {
                android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            try {
                pm.setComponentEnabledSetting(
                    android.content.ComponentName(packageName, alias),
                    enableState,
                    android.content.pm.PackageManager.DONT_KILL_APP
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
