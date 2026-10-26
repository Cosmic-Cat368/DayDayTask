// 顶层构建脚本：只声明插件版本，具体模块在 app/build.gradle.kts 中应用。
// 规格：Kotlin + Compose + Room + Navigation + Markwon + Coil + kotlinx-serialization + DataStore + Hilt。
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
