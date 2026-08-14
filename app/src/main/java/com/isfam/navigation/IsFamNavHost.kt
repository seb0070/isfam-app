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
import androidx.compose.runtime.mutableStateMapOf
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
import com.isfam.feature.auth.SignUpSession
import com.isfam.feature.auth.SignUpRoute
import com.isfam.feature.onboarding.OnboardingRoute
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
 *   ✅ 08 권한
 *   ✅ 09·10·11·12 목소리 등록
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
                    navController.navigate(Route.VoiceIntro) {
                        popUpTo(Route.SignUp) { inclusive = true }
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
            VoiceProcessingRoute(
                onComplete = {
                    navController.navigate(Route.VoiceComplete) {
                        popUpTo(Route.VoiceIntro) { inclusive = true }
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
            Placeholder(
                "13 가족 공간 진입 선택", navController,
                Route.FamilyCreate to "새로운 가족 공간 만들기",
                Route.InviteCodeInput to "초대 코드로 참여하기",
            )
        }
        composable<Route.FamilyCreate> {
            Placeholder("14 가족 공간 만들기", navController, Route.FamilyInvite to "만들고 초대하기")
        }
        composable<Route.FamilyInvite> {
            Placeholder("15 가족 초대", navController, Route.Home to "나중에 초대하고 홈으로")
        }
        composable<Route.FamilyInviteManage> {
            Placeholder("16 가족 초대 · 현황", navController)
        }
        composable<Route.InviteCodeInput> {
            Placeholder(
                "17 초대 코드 입력", navController,
                Route.InviteAccept("F7K-2M9") to "가족으로 연결하기",
            )
        }
        composable<Route.InviteAccept> { entry ->
            val args = entry.toRoute<Route.InviteAccept>()
            Placeholder("18 초대 수락\n${args.inviteCode}", navController, Route.VoiceIntro to "수락하고 목소리 등록")
        }

        // ── 3. 메인 ───────────────────────────────────────────
        composable<Route.Home> {
            Placeholder(
                "19 홈 대시보드", navController,
                Route.FamilyManage to "가족",
                Route.History to "기록",
                Route.Settings to "설정",
                Route.Analyzing(1) to "[테스트] 분석 중",
            )
        }
        composable<Route.FamilyManage> {
            Placeholder(
                "21 가족 관리", navController,
                Route.FamilyInviteManage to "가족 초대하기",
                Route.BlockedNumbers to "가족 차단 번호",
            )
        }
        composable<Route.History> {
            Placeholder("32 분석 기록", navController, Route.HistoryDetail(1) to "기록 상세")
        }
        composable<Route.Settings> {
            Placeholder("26 설정", navController)
        }
        composable<Route.BlockedNumbers> {
            Placeholder("24 가족 차단 번호", navController)
        }

        // ── 4. 분석 · 결과 ────────────────────────────────────
        composable<Route.Analyzing> {
            Placeholder("분석 중…", navController, Route.AnalysisResult(1) to "결과 보기")
        }
        composable<Route.AnalysisResult> {
            Placeholder(
                "28·29·30 결과", navController,
                Route.ShareDanger(1) to "가족에게 알리기",
                Route.History to "기록으로",
            )
        }
        composable<Route.ShareDanger> {
            Placeholder("31 가족 위험 공유", navController, Route.Home to "홈으로")
        }
        composable<Route.HistoryDetail> {
            Placeholder("33 기록 상세", navController, Route.Home to "홈으로")
        }
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