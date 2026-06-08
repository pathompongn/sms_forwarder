package com.nong.smsforwarder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class ForwardService : Service() {

    private val executor = Executors.newSingleThreadExecutor()
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val sender = intent?.getStringExtra(EXTRA_SENDER) ?: "Unknown"
        val body = intent?.getStringExtra(EXTRA_BODY) ?: ""

        showForegroundNotification("กำลังส่ง email...")
        acquireWakeLock()

        executor.execute {
            try {
                val settings = SettingsManager(this)
                val timestamp = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(Date())
                var lastException: Exception? = null
                var success = false
                for (attempt in 1..3) {
                    try {
                        EmailSender.send(settings, sender, body)
                        success = true
                        break
                    } catch (e: Exception) {
                        lastException = e
                        if (attempt < 3) Thread.sleep(5000)
                    }
                }
                if (success) {
                    settings.addLogEntry("$timestamp ✓ ส่งสำเร็จ | จาก: $sender | ${body.take(40)}...")
                    showResultNotification("ส่ง Email สำเร็จ", "จาก: $sender")
                } else {
                    settings.addLogEntry("$timestamp ✗ ส่งล้มเหลว (retry 3 ครั้งแล้ว) | ${lastException?.message}")
                    showResultNotification("ส่ง Email ล้มเหลว", lastException?.message ?: "ข้อผิดพลาดไม่ทราบสาเหตุ")
                }
            } finally {
                releaseWakeLock()
                stopSelf(startId)
            }
        }

        return START_NOT_STICKY
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "SmsForwarder:send_email"
            ).apply {
                acquire(2 * 60 * 1000L) // 2 นาที
            }
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showForegroundNotification(text: String) {
        createNotificationChannel()
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SMS Forwarder")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentIntent(pendingIntent)
            .build()
        startForeground(NOTIF_FORWARD_ID, notification)
    }

    private fun showResultNotification(title: String, text: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
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
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val EXTRA_SENDER = "sender"
        const val EXTRA_BODY = "body"
        private const val CHANNEL_ID = "sms_forwarder_channel"
        private const val NOTIF_FORWARD_ID = 1001
        private const val NOTIF_RESULT_ID = 1002
    }
}
