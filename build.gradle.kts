// 루트 build.gradle.kts  (프로젝트 최상단. app/ 안이 아닙니다)
//
// 플러그인 버전은 여기서만 지정하고, app/build.gradle.kts 에서는
// 버전 없이 id(...) 로만 적용합니다. 양쪽에 버전을 쓰면 충돌합니다.
//
// ⚠️ ksp 버전은 kotlin 버전과 앞자리가 반드시 일치해야 합니다.
//    kotlin 2.2.20 → ksp 2.2.20-x.y.z
//    맞지 않으면 "ksp is too old / too new" 오류가 납니다.

plugins {
    id("com.android.application") version "8.13.0" apply false
    id("org.jetbrains.kotlin.android") version "2.2.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.20" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.20" apply false
    id("com.google.devtools.ksp") version "2.2.20-2.0.2" apply false
}