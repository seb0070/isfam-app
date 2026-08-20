// app/build.gradle.kts
//
// 플러그인 버전은 루트 build.gradle.kts 에서만 지정합니다.
// 여기서 version 을 다시 쓰면 "already on the classpath" 충돌이 납니다.

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.isfam"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.isfam"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        // 서버 주소를 코드에 박지 마세요. 배포 환경마다 달라집니다.
        buildConfigField("String", "BASE_URL", "\"http://192.168.0.6:8080/\"")    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose BOM — 개별 Compose 라이브러리 버전을 자동으로 맞춰줍니다
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)

    // 통화 이벤트 저장 · 백그라운드 분석
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.androidx.work.runtime)

    // 온디바이스 화자 검증
    implementation(libs.onnxruntime.android)
    implementation(libs.jtransforms)
    testImplementation(libs.junit4)

    // implementation(libs.hilt.android)
    // ksp(libs.hilt.compiler)
    // implementation(libs.hilt.navigation.compose)

    // 초대 QR 생성.
    implementation(libs.zxing.core)
}
