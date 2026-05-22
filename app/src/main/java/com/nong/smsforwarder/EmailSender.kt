package com.nong.smsforwarder

import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object EmailSender {

    fun send(settings: SettingsManager, sender: String, body: String) {
        val props = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.host", settings.smtpHost)
            put("mail.smtp.port", settings.smtpPort)
            put("mail.smtp.connectiontimeout", "10000")
            put("mail.smtp.timeout", "10000")
        }

        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication =
                PasswordAuthentication(settings.smtpUser, settings.smtpPassword)
        })

        val message = MimeMessage(session).apply {
            setFrom(InternetAddress(settings.smtpUser, "SMS Forwarder"))
            setRecipients(Message.RecipientType.TO, InternetAddress.parse(settings.recipientEmail))
            subject = "[SMS] จาก: $sender"
            setText("ผู้ส่ง: $sender\n\n$body", "UTF-8")
        }

        Transport.send(message)
    }
}
