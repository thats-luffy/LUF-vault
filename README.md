# LUF Vault

<div dir="rtl" align="right">

## فارسی

**<bdi>LUF Vault</bdi>** یک اپلیکیشن اندرویدی برای مخفی‌سازی امن فایل‌های شخصی است که با <bdi>Kotlin</bdi> و <bdi>Jetpack Compose</bdi> توسعه یافته است.

### ویژگی‌ها

- رمزنگاری فایل‌ها با <bdi>AES-256</bdi> و ذخیره کلیدها در <bdi>Android Keystore</bdi>
- ورود با رمز عبور و پشتیبانی از احراز هویت بیومتریک
- قفل خودکار پس از چندین بار ورود ناموفق
- پشتیبانی از انواع فایل‌ها: تصویر، ویدیو، صدا، سند، آرشیو و <bdi>APK</bdi>
- گالری داخلی با نمایش دسته‌بندی‌شده و پخش‌کننده داخلی تصویر، ویدیو و صدا
- وارد کردن و خروجی گرفتن فایل‌ها (کپی یا انتقال)
- تنظیمات کامل شامل تم، زبان، قفل خودکار و محافظت از اسکرین‌شات
- معماری <bdi>MVVM</bdi> با <bdi>Room</bdi>، <bdi>Hilt</bdi>، <bdi>Coroutines</bdi> و <bdi>StateFlow</bdi>

</div>

---

## English

**LUF Vault** is an Android application for securely hiding personal files, built with **Kotlin** and **Jetpack Compose**.

### Features

- AES-256 file encryption with keys stored in Android Keystore
- Password login with optional biometric authentication
- Auto-lock after multiple failed login attempts
- Support for images, videos, audio, documents, archives, and APK files
- Built-in gallery with categorized tabs and media playback
- Import/export files (copy or move)
- Full settings: theme, language, auto-lock timeout, and screenshot protection
- MVVM architecture with Room, Hilt, Coroutines, and StateFlow
