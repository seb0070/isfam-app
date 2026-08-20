package com.isfam.core

import android.content.Context
import com.isfam.core.ml.MlContainer
import com.isfam.data.api.IsFamApi
import com.isfam.data.repository.AuthRepository
import com.isfam.data.repository.DeviceRepository
import com.isfam.data.repository.FakeAuthRepository
import com.isfam.data.repository.FakeDeviceRepository
import com.isfam.data.repository.FakeFamilyRepository
import com.isfam.data.repository.FakeNotificationRepository
import com.isfam.data.repository.FakeSettingsRepository
import com.isfam.data.repository.FakeVoiceprintRepository
import com.isfam.data.repository.FamilyRepository
import com.isfam.data.repository.NotificationRepository
import com.isfam.data.repository.RealAuthRepository
import com.isfam.data.repository.RealDeviceRepository
import com.isfam.data.repository.RealFamilyRepository
import com.isfam.data.repository.RealNotificationRepository
import com.isfam.data.repository.RealSettingsRepository
import com.isfam.data.repository.RealVoiceprintRepository
import com.isfam.data.repository.SettingsRepository
import com.isfam.data.repository.VoiceprintRepository
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * 경량 DI 컨테이너.
 *
 * Hilt 를 쓰지 않는 이유는 "제대로 안 하려고"가 아니라
 * 3일 안에 Hilt(KSP, @HiltViewModel, @AndroidEntryPoint) 설정 오류를
 * 디버깅할 여유가 없기 때문입니다.
 *
 * 이 구조를 지키면 나중에 Hilt 로 바꾸는 것은 기계적인 작업입니다.
 *   AppContainer 의 각 프로퍼티 → @Provides 함수
 *   생성자 주입은 이미 하고 있으므로 그대로 유지
 *
 * 중요한 건 "DI 프레임워크를 쓰는가"가 아니라
 * "화면이 구현체를 직접 만들지 않는가"입니다. 그건 지키고 있습니다.
 */
class AppContainer(private val context: Context) {

    /** 서버 준비 전에는 true. 준비되면 false 로만 바꾸면 됩니다. */
    var useFakeData: Boolean = false  // 수동

    private val json = Json {
        ignoreUnknownKeys = true      // 서버가 필드를 추가해도 앱이 죽지 않게
        coerceInputValues = true
        encodeDefaults = true
    }

    private val okHttp: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenStore))
            .addInterceptor(DeviceIdInterceptor(deviceIdStore))
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(com.isfam.BuildConfig.BASE_URL)
            .client(okHttp)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    val api: IsFamApi by lazy { retrofit.create(IsFamApi::class.java) }

    val tokenStore: TokenStore by lazy { TokenStore(context) }
    val deviceIdStore: DeviceIdStore by lazy { DeviceIdStore(context) }

    /**
     * ML 레이어. ONNX 세션이 무거워(모델 21MB) 앱 전체에서 하나만 씁니다.
     */
    val ml: MlContainer by lazy { MlContainer(context) }

    // ── Repository ────────────────────────────────────────────
    //
    // useFakeData 하나로 전체가 전환됩니다.
    // 화면은 인터페이스만 알고 구현체는 모르므로, 이 값을 바꿔도
    // 화면 코드는 한 줄도 수정할 필요가 없습니다.

    val authRepository: AuthRepository by lazy {
        if (useFakeData) FakeAuthRepository()
        else RealAuthRepository(api, tokenStore)
    }

    val familyRepository: FamilyRepository by lazy {
        if (useFakeData) FakeFamilyRepository() else RealFamilyRepository(api)
    }

    val voiceprintRepository: VoiceprintRepository by lazy {
        if (useFakeData) FakeVoiceprintRepository() else RealVoiceprintRepository(api)
    }

    val deviceRepository: DeviceRepository by lazy {
        if (useFakeData) FakeDeviceRepository() else RealDeviceRepository(api)
    }

    val settingsRepository: SettingsRepository by lazy {
        if (useFakeData) FakeSettingsRepository() else RealSettingsRepository(api)
    }

    val notificationRepository: NotificationRepository by lazy {
        if (useFakeData) FakeNotificationRepository() else RealNotificationRepository(api)
    }
}

/**
 * Application 에서 한 번만 만들고 전역에서 참조합니다.
 */
class IsFamApplication : android.app.Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    override fun onTerminate() {
        container.ml.close()
        super.onTerminate()
    }
}

/** Composable 어디서든 컨테이너를 꺼내는 헬퍼 */
@androidx.compose.runtime.Composable
fun rememberAppContainer(): AppContainer {
    val context = androidx.compose.ui.platform.LocalContext.current
    return (context.applicationContext as IsFamApplication).container
}

// ── 인터셉터 ──────────────────────────────────────────────────
//
// X-Device-Id 는 명세상 여러 엔드포인트에서 필수입니다.
// 화면마다 넘기면 반드시 빠뜨리는 곳이 생기므로 인터셉터로 자동 주입합니다.

class AuthInterceptor(private val tokenStore: TokenStore) : okhttp3.Interceptor {
    override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
        val token = tokenStore.accessToken
        val request = if (token.isNullOrBlank()) chain.request()
        else chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()
        return chain.proceed(request)
    }
}

class DeviceIdInterceptor(private val store: DeviceIdStore) : okhttp3.Interceptor {
    override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response =
        chain.proceed(
            chain.request().newBuilder()
                .addHeader("X-Device-Id", store.deviceId)
                .build()
        )
}

// ── 저장소 ────────────────────────────────────────────────────

class TokenStore(context: Context) {
    private val prefs = context.getSharedPreferences("isfam_auth", Context.MODE_PRIVATE)

    var accessToken: String?
        get() = prefs.getString("access_token", null)
        set(v) = prefs.edit().putString("access_token", v).apply()

    var refreshToken: String?
        get() = prefs.getString("refresh_token", null)
        set(v) = prefs.edit().putString("refresh_token", v).apply()

    /** 로그인·가입 성공 시 두 토큰을 한 번에 저장합니다 */
    fun save(access: String, refresh: String) {
        prefs.edit()
            .putString("access_token", access)
            .putString("refresh_token", refresh)
            .apply()
    }

    fun clear() = prefs.edit().clear().apply()
}

class DeviceIdStore(context: Context) {
    private val prefs = context.getSharedPreferences("isfam_device", Context.MODE_PRIVATE)

    /** 앱 최초 실행 시 한 번 생성해 계속 사용합니다. */
    val deviceId: String
        get() = prefs.getString("device_id", null) ?: java.util.UUID.randomUUID().toString()
            .also { prefs.edit().putString("device_id", it).apply() }
}