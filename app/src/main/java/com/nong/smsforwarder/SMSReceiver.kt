package com.nong.smsforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SMSReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val settings = SettingsManager(context)
        val ts = timestamp()

        settings.addLogEntry("$ts [1] SMS เข้า — Receiver ถูกเรียก")

        if (!settings.isEnabled) {
            settings.addLogEntry("$ts [!] หยุด: Toggle ปิดอยู่")
            return
        }
        if (!settings.isConfigured()) {
            settings.addLogEntry("$ts [!] หยุด: ยังไม่ได้ตั้งค่า email")
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) {
            settings.addLogEntry("$ts [!] หยุด: ไม่มีข้อความใน intent")
            return
        }

        val sender = messages[0].originatingAddress ?: "Unknown"
        val fullBody = messages.joinToString("") { it.messageBody }

        settings.addLogEntry("$ts [2] จาก: $sender | ${fullBody.take(30)}...")

        val keywords = settings.getKeywordList()
        val matched = keywords.isEmpty() || keywords.any { keyword ->
            fullBody.contains(keyword, ignoreCase = true) ||
            sender.contains(keyword, ignoreCase = true)
        }

        if (!matched) {
            settings.addLogEntry("$ts [!] หยุด: ไม่ตรง keyword (${keywords.joinToString(",")})")
            return
        }

        settings.addLogEntry("$ts [3] ตรง keyword — กำลัง forward...")

        val serviceIntent = Intent(context, ForwardService::class.java).apply {
            putExtra(ForwardService.EXTRA_SENDER, sender)
            putExtra(ForwardService.EXTRA_BODY, fullBody)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    private fun timestamp() =
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
}
