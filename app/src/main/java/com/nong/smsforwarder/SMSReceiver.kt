package com.nong.smsforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony

class SMSReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val settings = SettingsManager(context)
        if (!settings.isEnabled) return
        if (!settings.isConfigured()) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val sender = messages[0].originatingAddress ?: "Unknown"
        val fullBody = messages.joinToString("") { it.messageBody }

        val keywords = settings.getKeywordList()
        val matched = keywords.isEmpty() || keywords.any { keyword ->
            fullBody.contains(keyword, ignoreCase = true) ||
            sender.contains(keyword, ignoreCase = true)
        }

        if (!matched) return

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
}
