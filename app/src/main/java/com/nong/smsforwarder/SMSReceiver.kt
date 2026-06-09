package com.nong.smsforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.work.BackoffPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

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

        val data = workDataOf(
            ForwardWorker.KEY_SENDER to sender,
            ForwardWorker.KEY_BODY to fullBody
        )
        val request = OneTimeWorkRequestBuilder<ForwardWorker>()
            .setInputData(data)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }

    private fun timestamp() =
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
}
