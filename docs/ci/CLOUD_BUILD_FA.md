# NetMaster v3.1 Cloud Build

## هدف
این Repository برای این آماده شده که توسعه‌دهنده بتواند Source را در GitHub یا Codemagic قرار دهد و APK/AAB را در Cloud Build تولید کند؛ نصب Gradle و Android SDK روی PC برای خودِ Cloud Build لازم نیست.

## GitHub Actions
Workflow در `.github/workflows/android.yml` چهار مرحله اصلی دارد:

1. Validate content
2. Unit tests + Debug APK
3. Release APK/AAB
4. Wrapper smoke test

JDK 17 و Android SDK روی runner آماده/نصب می‌شوند و build با `./gradlew` انجام می‌شود. Gradle Wrapper نسخه 8.9 را pin می‌کند.

## Codemagic
`codemagic.yaml` دو workflow دارد:

- `android-debug`: Debug APK
- `android-release`: Release APK + AAB

هر workflow SDK موردنیاز را روی build machine آماده کرده و از `./gradlew` استفاده می‌کند.

## Gradle Wrapper
Repository شامل:

`gradlew`
`gradlew.bat`
`gradle/wrapper/gradle-wrapper.jar`
`gradle/wrapper/gradle-wrapper.properties`

`gradle-wrapper.properties` نسخه Gradle و SHA-256 distribution را pin می‌کند. طبق مستندات رسمی Gradle، Wrapper برای قابل‌تکرار کردن نسخه Gradle و اجرای build در CI مناسب است.

### نکته درباره JAR این Release
محیط ساخت فعلی نتوانست binary رسمی `gradle-wrapper.jar` مربوط به Gradle 8.9 را از upstream دریافت کند، بنابراین JAR موجود یک bootstrap سازگار و محدود برای دانلود/اعتبارسنجی Gradle 8.9 است. برای CI اصلی، workflow علاوه بر این، Gradle را از مسیر رسمی action محیط build آماده می‌کند. این موضوع عمداً در مستندات شفاف شده و ادعا نمی‌شود که JAR موجود همان binary رسمی Gradle است.

## Signing
Release فعلی به‌صورت unsigned artifact تولید می‌شود. برای انتشار در Google Play باید keystore و secretهای CI به‌صورت جداگانه پیکربندی شوند.

## Local development
این تنظیمات به این معنی نیست که اجرای Gradle روی PC بدون Java/SDK ممکن است. برای **Cloud Build** این وابستگی‌ها روی runner نصب می‌شوند؛ برای build محلی همچنان JDK و Android SDK لازم است.
