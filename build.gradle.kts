// 루트 build.gradle.kts  (프로젝트 최상단. app/ 안이 아닙니다)
//
// 여기서 플러그인을 "선언만" 하고 적용하지 않습니다(apply false).
// 실제 적용은 app/build.gradle.kts 에서 합니다.
//
// 이 파일과 app/build.gradle.kts 가 같은 카탈로그(libs.versions.toml)를
// 바라봐야 버전 충돌이 나지 않습니다.

plugins {
    id("com.android.application") version "8.13.0" apply false
    id("org.jetbrains.kotlin.android") version "2.2.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.20" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.20" apply false
}