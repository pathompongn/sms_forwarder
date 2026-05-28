# SMS Forwarder

Android app ที่รับ SMS แล้ว forward ไปยัง Email โดยอัตโนมัติ รองรับการกรอง keyword และส่งผ่าน SMTP (Gmail)

## วิธีทำงาน

```
SMS เข้า → SMSReceiver → ตรวจ keyword → ForwardService → EmailSender (SMTP)
```

1. `SMSReceiver` รับ broadcast เมื่อมี SMS เข้า
2. ตรวจสอบว่า forwarding เปิดอยู่ และมี keyword ตรงหรือไม่
3. `ForwardService` รัน background task ส่ง email ผ่าน SMTP
4. ถ้าส่งไม่สำเร็จ จะ retry อีก 3 รอบ รอ 5 วินาทีต่อรอบ
5. `BootReceiver` ทำให้ app พร้อมรับ SMS หลัง reboot

## การตั้งค่า

| ฟิลด์ | ตัวอย่าง | หมายเหตุ |
|---|---|---|
| Email ปลายทาง | `you@gmail.com` | email ที่รับ SMS ที่ forward มา |
| Gmail ผู้ส่ง | `sender@gmail.com` | Gmail ที่ใช้ส่ง ต้องเปิด 2FA |
| App Password | `xxxx xxxx xxxx xxxx` | 16 ตัวอักษร จาก Google Account |
| Keywords | `OTP,รหัส,KBANK` | คั่นด้วย `,` — ถ้าเว้นว่าง = forward ทุก SMS |

### ขอ Gmail App Password

1. ไปที่ [myaccount.google.com](https://myaccount.google.com) → Security
2. เปิด **2-Step Verification** ก่อน
3. ค้นหา **App passwords** → สร้าง password ใหม่
4. Copy 16 ตัวอักษรไปใส่ใน app

## Permissions ที่ใช้

| Permission | เหตุผล |
|---|---|
| `RECEIVE_SMS` / `READ_SMS` | รับและอ่าน SMS |
| `INTERNET` | ส่ง email ผ่าน SMTP |
| `RECEIVE_BOOT_COMPLETED` | พร้อมทำงานหลัง reboot |
| `FOREGROUND_SERVICE` | รัน background task ส่ง email |
| `POST_NOTIFICATIONS` | แสดงแจ้งเตือนผลการส่ง (Android 13+) |

## Build

**Android Studio**
1. Open → เลือกโฟลเดอร์ `SmsForwarder`
2. รอ Gradle sync เสร็จ
3. Build → Build APK(s)
4. APK: `app/build/outputs/apk/debug/app-debug.apk`

**Command Line**
```bash
.\gradlew assembleDebug
```

**ติดตั้งผ่าน ADB**
```bash
adb install app\build\outputs\apk\debug\app-debug.apk
```

## โครงสร้างไฟล์

```
app/src/main/java/com/nong/smsforwarder/
├── MainActivity.kt       # UI หลัก — ตั้งค่า, ทดสอบ, log
├── SMSReceiver.kt        # รับ SMS broadcast, กรอง keyword
├── ForwardService.kt     # Background service ส่ง email + retry logic
├── EmailSender.kt        # ส่ง email ผ่าน JavaMail (SMTP)
├── SettingsManager.kt    # SharedPreferences wrapper
└── BootReceiver.kt       # พร้อมทำงานหลัง reboot
```

## Requirements

- Android 8.0 (API 26) ขึ้นไป
- Gmail พร้อม 2-Step Verification และ App Password
