plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.example.taskrecall"
    compileSdk = 34

    // 本机 SDK 只安装了 35.0.0 的 Build-Tools；显式指定可避免 AGP 默认回退到 34.0.0。
    // 这不改变规格：targetSdk 仍固定为 34。
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "com.example.taskrecall"
        minSdk = 26
        targetSdk = 34
        versionCode = 10
        versionName = "1.4.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // 规格未要求混淆；为保证 zip 导入导出中的 kotlinx-serialization 反射安全，先不开启。
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true

            // Robolectric 默认把下载锁和 android-all 缓存放到 user.home；
            // 在本机沙箱下该目录不可写，因此重定向到项目内（已 gitignore）。
            all {
                val testHome = rootProject.layout.projectDirectory.dir(".test-home").asFile
                it.systemProperty("user.home", testHome.absolutePath)
                it.systemProperty(
                    "robolectric.dependency.repo.url",
                    "https://repo1.maven.org/maven2",
                )
            }
        }
    }
}

ksp {
    // Room 导出 schema，便于后续检查两张表的结构与数据库迁移。
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)

    // Compose + Material 3
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.androidx.compose.material.icons.extended)

    // Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    // Hilt（依赖注入）
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Room（SQLite）
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // 设置持久化
    implementation(libs.androidx.datastore.preferences)

    // JSON 序列化（导入导出）
    implementation(libs.kotlinx.serialization.json)

    // 图片加载；Markwon 渲染 Markdown
    implementation(libs.coil.compose)
    implementation(libs.markwon.core)
    implementation(libs.markwon.image)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
