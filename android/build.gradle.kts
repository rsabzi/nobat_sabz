plugins {
    // AGP 8.6 برای compileSdk 35 لازم است و با Gradle 8.7 (نسخه‌ی CI) سازگار است
    id("com.android.application") version "8.6.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.24" apply false
}
