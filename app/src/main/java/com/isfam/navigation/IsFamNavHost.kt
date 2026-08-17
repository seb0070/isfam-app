package com.isfam.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.isfam.core.designsystem.IsFamButton
import com.isfam.core.designsystem.IsFamOutlinedButton
import com.isfam.feature.auth.LoginRoute
import com.isfam.core.designsystem.MainTab
import com.isfam.core.designsystem.IsFamToast
import com.isfam.feature.family.FamilyCreateRoute
import com.isfam.feature.family.FamilyManageRoute
import com.isfam.feature.family.FamilyMemberItem
import com.isfam.feature.family.MemberProfileSheet
import com.isfam.feature.family.FamilyEntryRoute
import com.isfam.feature.family.FamilyInviteRoute
import com.isfam.feature.family.InviteAcceptRoute
import com.isfam.feature.family.InviteCodeInputRoute
import com.isfam.feature.family.InvitePreview
import com.isfam.feature.auth.SignUpSession
import com.isfam.feature.auth.SignUpRoute
import com.isfam.core.permission.SettingsIntents
import com.isfam.feature.history.HistoryDetailRoute
import com.isfam.feature.history.HistoryRoute
import com.isfam.feature.history.HistorySegment
import com.isfam.feature.home.HomeRoute
import com.isfam.feature.home.MemberStatus
import com.isfam.feature.home.RegistrationRequestSheet
import com.isfam.feature.onboarding.CallRecordingSetupRoute
import com.isfam.feature.onboarding.OnboardingRoute
import com.isfam.feature.result.AnalysisResultRoute
import com.isfam.feature.settings.SettingsRoute
import com.isfam.feature.onboarding.PermissionRoute
import com.isfam.feature.splash.SplashRoute
import com.isfam.feature.voice.VoiceCompleteRoute
import com.isfam.feature.voice.VoiceIntroRoute
import com.isfam.feature.voice.VoiceProcessingRoute
import com.isfam.feature.voice.VoiceRecordRoute

/**
 * 화면 이동 그래프.
 *
 * 완성된 화면은 실제 Route Composable 로,
 * 아직 안 만든 화면은 Placeholder 로 연결해 둡니다.
 * 화면이 완성될 때마다 Placeholder 자리를 하나씩 바꿔 끼우면 됩니다.
 *
 * 진행 상황
 *   ✅ 01 스플래시
 *   ✅ 02·03·04 온보딩
 *   ✅ 05 로그인 · 06 회원가입 · 07 OTP
 *   ✅ 08 권한 · 08-b 자동녹음·배터리 안내
 *   ✅ 09·10·11·12 목소리 등록
 *   ✅ 13·14·15·16·17·18 가족 공간 · 초대
 *   ✅ 19·20 홈 · 등록 요청 시트
 *   ✅ 21·22·23 가족 관리 · 프로필 시트 · 토스트
 *   ✅ 24·25·32 기록 탭 (분석 기록 · 차단 번호)
 *   ✅ 26 설정
 *   ✅ 28·29·30 분석 결과
 *   ✅ 33 기록 상세
 *   ⬜ 나머지
 */
@Composable
fun IsFamNavHost(
    navController: NavHostController = rememberNavController(),
) {
    // 회원가입 정보는 06과 08 두 화면에 걸쳐 모입니다.
    // 권한 상태가 signup 요청에 포함되므로 실제 API 호출은 08에서 일어납니다.
    val signUpSession = remember { SignUpSession() }

    // 문장별 녹음 파일. 3개가 모이면 서버로 업로드합니다.
    val recordedVoiceFiles = remember { mutableStateMapOf<Int, java.io.File>() }

    NavHost(
        navController = navController,
        startDestination = Route.Splash,
    ) {
        // ── 1. 진입 · 인증 ────────────────────────────────────
        composable<Route.Splash> {
            SplashRoute(
                onLoggedIn = {
                    navController.navigate(Route.Home) {
                        popUpTo(Route.Splash) { inclusive = true }
                    }
                },
                onNeedLogin = {
                    navController.navigate(Route.Onboarding) {
                        popUpTo(Route.Splash) { inclusive = true }
                    }
                },
            )
        }

        composable<Route.Onboarding> {
            OnboardingRoute(
                onFinish = {
                    navController.navigate(Route.Login) {
                        popUpTo(Route.Onboarding) { inclusive = true }
                    }
                },
            )
        }
        composable<Route.Login> {
            LoginRoute(
                onLoginSuccess = {
                    navController.navigate(Route.Home) {
                        popUpTo(Route.Login) { inclusive = true }
                    }
                },
                onSignUp = { navController.navigate(Route.SignUp) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<Route.SignUp> {
            SignUpRoute(
                onNext = { form ->
                    signUpSession.applyForm(form)
                    navController.navigate(Route.Permission)
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable<Route.Permission> {
            PermissionRoute(
                session = signUpSession,
                onSignUpComplete = {
                    navController.navigate(Route.CallRecordingSetup) {
                        popUpTo(Route.SignUp) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable<Route.CallRecordingSetup> {
            CallRecordingSetupRoute(
                onNext = { callRecordingEnabled ->
                    signUpSession.callRecordingEnabled = callRecordingEnabled
                    // TODO: PUT /devices/me/capability 로 자기 신고값 전송
                    navController.navigate(Route.VoiceIntro) {
                        popUpTo(Route.CallRecordingSetup) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        // ── 2. 가족 · 목소리 등록 ─────────────────────────────
        composable<Route.VoiceIntro> {
            VoiceIntroRoute(
                onStart = { navController.navigate(Route.VoiceRecord(1)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<Route.VoiceRecord> { entry ->
            val args = entry.toRoute<Route.VoiceRecord>()
            VoiceRecordRoute(
                sentenceIndex = args.sentenceIndex,
                onComplete = { file ->
                    recordedVoiceFiles[args.sentenceIndex] = file
                    if (args.sentenceIndex < 3) {
                        navController.navigate(Route.VoiceRecord(args.sentenceIndex + 1))
                    } else {
                        navController.navigate(Route.VoiceProcessing) {
                            popUpTo(Route.VoiceIntro)
                        }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable<Route.VoiceProcessing> {
            // 컴포지션마다 새 List 가 만들어지지 않도록 고정합니다.
            // 매번 새 인스턴스면 LaunchedEffect 가 반복 실행됩니다.
            val filesForEnrollment = remember {
                recordedVoiceFiles.entries.sortedBy { it.key }.map { it.value }
            }

            VoiceProcessingRoute(
                recordedFiles = filesForEnrollment,
                onComplete = {
                    recordedVoiceFiles.clear()
                    navController.navigate(Route.VoiceComplete) {
                        popUpTo(Route.VoiceProcessing) { inclusive = true }
                    }
                },
                onFailed = {
                    // 음질 미달 등으로 실패하면 첫 문장부터 다시 녹음합니다
                    recordedVoiceFiles.clear()
                    navController.navigate(Route.VoiceRecord(1)) {
                        popUpTo(Route.VoiceProcessing) { inclusive = true }
                    }
                },
            )
        }
        composable<Route.VoiceComplete> {
            VoiceCompleteRoute(
                displayName = signUpSession.displayName.ifBlank { "회원" },
                onNext = { navController.navigate(Route.FamilyEntry) },
            )
        }
        composable<Route.FamilyEntry> {
            FamilyEntryRoute(
                onCreate = { navController.navigate(Route.FamilyCreate) },
                onJoin = { navController.navigate(Route.InviteCodeInput) },
            )
        }
        composable<Route.FamilyCreate> {
            FamilyCreateRoute(
                ownerName = signUpSession.userName.ifBlank { "회원" },
                onCreated = { navController.navigate(Route.FamilyInvite) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<Route.FamilyInvite> {
            FamilyInviteRoute(
                // TODO: POST /api/v1/family/invite-code 응답으로 교체
                inviteCode = "AB12CD",
                qrCodeUrl = null,
                expiresInText = "유효시간 71시간 58분 남음",
                onSkip = {
                    navController.navigate(Route.Home) {
                        popUpTo(Route.FamilyEntry) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable<Route.FamilyInviteManage> {
            FamilyInviteRoute(
                inviteCode = "AB12CD",
                qrCodeUrl = null,
                expiresInText = "유효시간 71시간 58분 남음",
                isReentry = true,
                onSkip = null,
                onBack = { navController.popBackStack() },
            )
        }
        composable<Route.InviteCodeInput> {
            InviteCodeInputRoute(
                onVerified = { code -> navController.navigate(Route.InviteAccept(code)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<Route.InviteAccept> { entry ->
            val args = entry.toRoute<Route.InviteAccept>()
            InviteAcceptRoute(
                // TODO: GET /api/v1/invitations/{code} 응답으로 교체
                preview = InvitePreview(
                    inviterName = "김상호",
                    spaceName = "김서연님의 가족 공간",
                    memberInitials = listOf("상", "서"),
                    enteredByLink = args.inviteCode.isBlank(),
                ),
                myInitial = signUpSession.displayName.take(1).ifBlank { "나" },
                onAccepted = {
                    navController.navigate(Route.VoiceIntro) {
                        popUpTo(Route.FamilyEntry) { inclusive = true }
                    }
                },
                onDecline = { navController.popBackStack() },
            )
        }

        // ── 3. 메인 ───────────────────────────────────────────
        composable<Route.Home> {
            var sheetMember by remember { mutableStateOf<MemberStatus?>(null) }

            HomeRoute(
                onTabSelected = { tab -> navController.navigateToTab(tab) },
                onAnalysisClick = { id -> navController.navigate(Route.AnalysisResult(id)) },
                onInvite = { navController.navigate(Route.FamilyInviteManage) },
                onBlockedNumbers = { navController.navigate(Route.History(showBlocked = true)) },
                onRequestRegistration = { sheetMember = it },
                onSeeAllHistory = { navController.navigate(Route.History()) },
            )

            // 20. 등록 요청 바텀시트
            sheetMember?.let { member ->
                RegistrationRequestSheet(
                    member = member,
                    onKakaoRequest = { sheetMember = null },   // TODO: 카카오 SDK
                    onPushRequest = { sheetMember = null },    // TODO: 서버 알림 발송
                    onDismiss = { sheetMember = null },
                )
            }
        }
        composable<Route.FamilyManage> {
            var profileMember by remember { mutableStateOf<FamilyMemberItem?>(null) }
            var toastMessage by remember { mutableStateOf<String?>(null) }

            Box {
                FamilyManageRoute(
                    onTabSelected = { tab -> navController.navigateToTab(tab) },
                    onInvite = { navController.navigate(Route.FamilyInviteManage) },
                    onBlockedNumbers = { navController.navigate(Route.History(showBlocked = true)) },
                    onMemberClick = { profileMember = it },
                    onRequestRegistration = { member ->
                        // TODO: 서버 알림 발송
                        toastMessage = "${member.name}님에게 등록 요청을 보냈어요."
                    },
                )

                // 23. 토스트
                toastMessage?.let { message ->
                    LaunchedEffect(message) {
                        kotlinx.coroutines.delay(2500)
                        toastMessage = null
                    }
                    IsFamToast(
                        message = message,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(start = 20.dp, end = 20.dp, bottom = 120.dp),
                    )
                }
            }

            // 22. 프로필 바텀시트
            profileMember?.let { member ->
                MemberProfileSheet(
                    member = member,
                    onSave = { displayName, autoAnalysis ->
                        profileMember = null
                        if (!autoAnalysis) {
                            toastMessage = "${displayName}님과의 통화는 자동 분석 대상에서 제외됩니다."
                        }
                    },
                    onDismiss = { profileMember = null },
                )
            }
        }
        composable<Route.History> { entry ->
            val args = entry.toRoute<Route.History>()
            HistoryRoute(
                initialSegment =
                    if (args.showBlocked) HistorySegment.Blocked else HistorySegment.Analysis,
                onTabSelected = { tab -> navController.navigateToTab(tab) },
                onItemClick = { id -> navController.navigate(Route.HistoryDetail(id)) },
            )
        }
        composable<Route.Settings> {
            val context = androidx.compose.ui.platform.LocalContext.current
            SettingsRoute(
                onTabSelected = { tab -> navController.navigateToTab(tab) },
                onMyInfo = { /* TODO: 내 정보 화면 */ },
                onVoiceManage = { navController.navigate(Route.VoiceIntro) },
                onFamilyManage = { navController.navigateToTab(MainTab.Family) },
                onOpenSystemSettings = {
                    SettingsIntents.safeStart(context, SettingsIntents.appDetails(context))
                },
                onVoiceprintStorage = { /* TODO: 성문 보관 안내 */ },
                onDeleteAllVoiceData = { /* TODO: 삭제 확인 다이얼로그 */ },
                onLogout = {
                    navController.navigate(Route.Login) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        // ── 4. 분석 · 결과 ────────────────────────────────────
        composable<Route.Analyzing> {
            Placeholder("분석 중…", navController, Route.AnalysisResult(1) to "결과 보기")
        }
        composable<Route.AnalysisResult> { entry ->
            val args = entry.toRoute<Route.AnalysisResult>()
            AnalysisResultRoute(
                analysisId = args.analysisId,
                onClose = { navController.popBackStack() },
                onShareToFamily = { id -> navController.navigate(Route.ShareDanger(id)) },
                onBlockAndReport = { /* TODO: 차단 목록 추가 + 신고 안내 */ },
                onSeeDetail = { id -> navController.navigate(Route.HistoryDetail(id)) },
            )
        }
        composable<Route.ShareDanger> {
            Placeholder("31 가족 위험 공유", navController, Route.Home to "홈으로")
        }
        composable<Route.HistoryDetail> { entry ->
            val args = entry.toRoute<Route.HistoryDetail>()
            HistoryDetailRoute(
                analysisId = args.analysisId,
                onBack = { navController.popBackStack() },
                onDelete = {
                    // TODO: DELETE /voice-analyses/{id}
                    navController.popBackStack()
                },
                onReportGuide = { /* TODO: 경찰청 신고 안내 */ },
                onHome = {
                    navController.navigate(Route.Home) { popUpTo(Route.Home) }
                },
            )
        }
    }
}

/**
 * 하단 탭 이동.
 * 탭 사이를 오갈 때 백스택이 쌓이지 않도록 홈까지 정리합니다.
 */
private fun NavHostController.navigateToTab(tab: MainTab) {
    val route: Route = when (tab) {
        MainTab.Home -> Route.Home
        MainTab.Family -> Route.FamilyManage
        MainTab.History -> Route.History()
        MainTab.Settings -> Route.Settings
    }
    navigate(route) {
        popUpTo(Route.Home) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** 아직 만들지 않은 화면 자리 */
@Composable
private fun Placeholder(
    title: String,
    navController: NavHostController,
    vararg actions: Pair<Route, String>,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)

        actions.forEach { (route, label) ->
            IsFamButton(text = label, onClick = { navController.navigate(route) })
        }

        if (navController.previousBackStackEntry != null) {
            IsFamOutlinedButton(text = "뒤로", onClick = { navController.popBackStack() })
        }
    }
}