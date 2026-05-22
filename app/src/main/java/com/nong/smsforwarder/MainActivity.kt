package com.nong.smsforwarder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.nong.smsforwarder.databinding.ActivityMainBinding
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var settings: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        settings = SettingsManager(this)
        loadSettings()
        requestRequiredPermissions()

        binding.btnSave.setOnClickListener { saveSettings() }
        binding.btnTest.setOnClickListener { sendTestEmail() }
        binding.switchEnabled.setOnCheckedChangeListener { _, isChecked ->
            settings.isEnabled = isChecked
            updateStatusText()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshLog()
    }

    private fun loadSettings() {
        binding.etRecipient.setText(settings.recipientEmail)
        binding.etSmtpUser.setText(settings.smtpUser)
        binding.etSmtpPassword.setText(settings.smtpPassword)
        binding.etKeywords.setText(settings.keywords)
        binding.switchEnabled.isChecked = settings.isEnabled
        updateStatusText()
        refreshLog()
    }

    private fun saveSettings() {
        val recipient = binding.etRecipient.text.toString().trim()
        val smtpUser = binding.etSmtpUser.text.toString().trim()
        val smtpPass = binding.etSmtpPassword.text.toString().trim()
        val keywords = binding.etKeywords.text.toString().trim()

        if (recipient.isEmpty() || smtpUser.isEmpty() || smtpPass.isEmpty()) {
            Toast.makeText(this, "กรุณากรอก Email และ App Password ให้ครบ", Toast.LENGTH_SHORT).show()
            return
        }

        settings.recipientEmail = recipient
        settings.smtpUser = smtpUser
        settings.smtpPassword = smtpPass
        settings.keywords = keywords

        Toast.makeText(this, "บันทึกการตั้งค่าสำเร็จ", Toast.LENGTH_SHORT).show()
        updateStatusText()
    }

    private fun sendTestEmail() {
        saveSettings()
        if (!settings.isConfigured()) {
            Toast.makeText(this, "กรุณากรอกการตั้งค่าให้ครบก่อน", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnTest.isEnabled = false
        binding.tvLog.text = "กำลังส่ง email ทดสอบ..."

        Executors.newSingleThreadExecutor().execute {
            try {
                EmailSender.send(settings, "TEST", "นี่คือ email ทดสอบจาก SMS Forwarder\n\nหากคุณได้รับ email นี้ แปลว่าการตั้งค่า SMTP ถูกต้อง")
                runOnUiThread {
                    Toast.makeText(this, "ส่ง email ทดสอบสำเร็จ! ตรวจสอบกล่องขาเข้าครับ", Toast.LENGTH_LONG).show()
                    binding.btnTest.isEnabled = true
                    settings.addLogEntry("${currentTimestamp()} ✓ ทดสอบส่ง email สำเร็จ")
                    refreshLog()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "ส่งล้มเหลว: ${e.message}", Toast.LENGTH_LONG).show()
                    binding.btnTest.isEnabled = true
                    settings.addLogEntry("${currentTimestamp()} ✗ ทดสอบล้มเหลว: ${e.message}")
                    refreshLog()
                }
            }
        }
    }

    private fun updateStatusText() {
        val enabled = settings.isEnabled
        val configured = settings.isConfigured()
        binding.tvStatus.text = when {
            !configured -> "สถานะ: ยังไม่ได้ตั้งค่า — กรุณากรอกข้อมูลและบันทึก"
            enabled -> "สถานะ: กำลังทำงาน — รอรับ SMS..."
            else -> "สถานะ: ปิดอยู่"
        }
    }

    private fun refreshLog() {
        val log = settings.getLog()
        binding.tvLog.text = if (log.isEmpty()) "ยังไม่มีประวัติการส่ง" else log
    }

    private fun currentTimestamp(): String {
        val sdf = java.text.SimpleDateFormat("dd/MM/yy HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }

    private fun requestRequiredPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), REQUEST_PERMISSIONS)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS) {
            val denied = permissions.zip(grantResults.toList())
                .filter { it.second != PackageManager.PERMISSION_GRANTED }
            if (denied.isNotEmpty()) {
                Toast.makeText(
                    this,
                    "แอปต้องการสิทธิ์รับ SMS เพื่อทำงาน กรุณาอนุญาตใน Settings",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    companion object {
        private const val REQUEST_PERMISSIONS = 100
    }
}
