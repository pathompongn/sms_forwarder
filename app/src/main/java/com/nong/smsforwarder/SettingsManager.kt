package com.nong.smsforwarder

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("sms_forwarder_prefs", Context.MODE_PRIVATE)

    var isEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    var recipientEmail: String
        get() = prefs.getString(KEY_RECIPIENT, "") ?: ""
        set(value) = prefs.edit().putString(KEY_RECIPIENT, value).apply()

    var smtpHost: String
        get() = prefs.getString(KEY_SMTP_HOST, "smtp.gmail.com") ?: "smtp.gmail.com"
        set(value) = prefs.edit().putString(KEY_SMTP_HOST, value).apply()

    var smtpPort: String
        get() = prefs.getString(KEY_SMTP_PORT, "587") ?: "587"
        set(value) = prefs.edit().putString(KEY_SMTP_PORT, value).apply()

    var smtpUser: String
        get() = prefs.getString(KEY_SMTP_USER, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SMTP_USER, value).apply()

    var smtpPassword: String
        get() = prefs.getString(KEY_SMTP_PASS, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SMTP_PASS, value).apply()

    var keywords: String
        get() = prefs.getString(KEY_KEYWORDS, "") ?: ""
        set(value) = prefs.edit().putString(KEY_KEYWORDS, value).apply()

    fun getKeywordList(): List<String> =
        keywords.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    fun addLogEntry(entry: String) {
        val current = prefs.getString(KEY_LOG, "") ?: ""
        val lines = current.lines().toMutableList()
        lines.add(0, entry)
        val trimmed = lines.take(20).joinToString("\n")
        prefs.edit().putString(KEY_LOG, trimmed).apply()
    }

    fun getLog(): String = prefs.getString(KEY_LOG, "") ?: ""

    fun isConfigured(): Boolean =
        smtpUser.isNotEmpty() && smtpPassword.isNotEmpty() && recipientEmail.isNotEmpty()

    companion object {
        private const val KEY_ENABLED = "enabled"
        private const val KEY_RECIPIENT = "recipient_email"
        private const val KEY_SMTP_HOST = "smtp_host"
        private const val KEY_SMTP_PORT = "smtp_port"
        private const val KEY_SMTP_USER = "smtp_user"
        private const val KEY_SMTP_PASS = "smtp_password"
        private const val KEY_KEYWORDS = "keywords"
        private const val KEY_LOG = "log"
    }
}
