// app/build.gradle.kts
//
// 3일 스프린트용 단일 모듈 설정입니다.
// Hilt / Room 은 주석 처리해 뒀습니다. 필요해지면 주석만 풀면 됩니다.

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
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
        buildConfigField("String", "BASE_URL", "\"https://api.isfam.example/\"")
    }

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

    // 가족 화자 검증은 서버가 아니라 기기 안에서 실행합니다.
    implementation(libs.onnxruntime.android)
    implementation(libs.jtransforms)
    testImplementation(libs.junit4)

    // implementation(libs.hilt.android)
    // ksp(libs.hilt.compiler)
    // implementation(libs.hilt.navigation.compose)
    // implementation(libs.room.runtime)
    // implementation(libs.room.ktx)
    // ksp(libs.room.compiler)
}
