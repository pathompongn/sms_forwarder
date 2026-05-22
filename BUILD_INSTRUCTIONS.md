# วิธี Build SMS Forwarder APK

## วิธีที่ 1: ใช้ Android Studio (แนะนำ)
1. เปิด Android Studio → Open → เลือกโฟลเดอร์ `SmsForwarder`
2. รอให้ Gradle sync เสร็จ
3. Build → Build Bundle(s)/APK(s) → Build APK(s)
4. APK อยู่ที่: `app/build/outputs/apk/debug/app-debug.apk`

## วิธีที่ 2: Command Line
```bash
# ต้องมี Android SDK และ ANDROID_HOME set ไว้
cd SmsForwarder
.\gradlew assembleDebug
```

## ติดตั้ง APK บน Android
```bash
# ผ่าน ADB
adb install app\build\outputs\apk\debug\app-debug.apk

# หรือ copy ไฟล์ APK ไปไว้ใน Android แล้วเปิดติดตั้งเอง
# (ต้องเปิด "Install unknown apps" ใน Settings ก่อน)
```

## การตั้งค่า Gmail App Password
1. ไปที่ https://myaccount.google.com
2. Security → 2-Step Verification (ต้องเปิดก่อน)
3. App passwords → สร้าง password ใหม่
4. Copy 16-character password ไปใส่ใน app

## การตั้งค่าใน App
1. **Email ปลายทาง**: email ที่ต้องการรับ SMS ที่ forward มา
2. **Gmail ผู้ส่ง**: Gmail ที่ใช้ส่ง (yourname@gmail.com)
3. **App Password**: 16-character password จาก Google Account
4. **Keywords**: คำที่ต้องการกรอง เช่น `OTP,รหัส,ยืนยัน,KBANK`
   - ถ้าเว้นว่าง = forward ทุก SMS
5. กด **บันทึก** แล้วกด **ทดสอบส่ง Email** เพื่อตรวจสอบ
6. เปิด Toggle **เปิด/ปิด Forwarding**
