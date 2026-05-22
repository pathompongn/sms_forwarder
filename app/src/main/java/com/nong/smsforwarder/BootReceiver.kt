package com.nong.smsforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.LOCKED_BOOT_COMPLETED"
        ) {
            val settings = SettingsManager(context)
            settings.addLogEntry("${currentTimestamp()} เครื่องรีสตาร์ท — SMS Forwarder พร้อมทำงาน (เปิด: ${settings.isEnabled})")
        }
    }

    private fun currentTimestamp(): String {
        val sdf = java.text.SimpleDateFormat("dd/MM/yy HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }
}
