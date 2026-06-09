package com.nong.smsforwarder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ส่ง email ผ่าน WorkManager แทน foreground service
 *
 * เหตุผล: Android 12+ ห้าม start foreground service จาก background (เช่นตอนจอดับ/Doze)
 * ทำให้ ForwardService เดิมตายก่อนได้ส่ง email. WorkManager expedited work
 * ได้รับ exemption ให้ทำงานจาก background ได้ และจัดการ wake lock / retry ให้เอง
 */
class ForwardWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val sender = inputData.getString(KEY_SENDER) ?: "Unknown"
        val body = inputData.getString(KEY_BODY) ?: ""
        val settings = SettingsManager(applicationContext)
        val timestamp = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(Date())

        return try {
            EmailSender.send(settings, sender, body)
            settings.addLogEntry("$timestamp ✓ ส่งสำเร็จ | จาก: $sender | ${body.take(40)}...")
            showResultNotification("ส่ง Email สำเร็จ", "จาก: $sender")
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < MAX_ATTEMPTS - 1) {
                settings.addLogEntry("$timestamp … ส่งล้มเหลว (ครั้งที่ ${runAttemptCount + 1}) จะลองใหม่ | ${e.message}")
                Result.retry()
            } else {
                settings.addLogEntry("$timestamp ✗ ส่งล้มเหลว (retry $MAX_ATTEMPTS ครั้งแล้ว) | ${e.message}")
                showResultNotification("ส่ง Email ล้มเหลว", e.message ?: "ข้อผิดพลาดไม่ทราบสาเหตุ")
                Result.failure()
            }
        }
    }

    override fun getForegroundInfo(): ForegroundInfo {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("SMS Forwarder")
            .setContentText("กำลังส่ง email...")
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ForegroundInfo(NOTIF_FORWARD_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIF_FORWARD_ID, notification)
        }
    }

    private fun showResultNotification(title: String, text: String) {
        createNotificationChannel()
        val manager = applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIF_RESULT_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SMS Forwarder",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "แจ้งเตือนการส่ง Email"
            }
            val manager = applicationContext
                .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val KEY_SENDER = "sender"
        const val KEY_BODY = "body"
        private const val CHANNEL_ID = "sms_forwarder_channel"
        private const val NOTIF_FORWARD_ID = 1001
        private const val NOTIF_RESULT_ID = 1002
        private const val MAX_ATTEMPTS = 3
    }
}
