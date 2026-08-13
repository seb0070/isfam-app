package com.isfam.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute

@Composable
fun IsFamNavHost(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Route.Splash,
    ) {
        // ── 온보딩 ────────────────────────────────────────────
        composable<Route.Splash> {
            Placeholder("Splash", navController, Route.Intro to "시작")
        }
        composable<Route.Intro> {
            Placeholder("서비스 소개", navController, Route.RoleSelect to "다음")
        }
        composable<Route.RoleSelect> {
            Placeholder(
                "역할 선택", navController,
                Route.PhoneInput to "자녀로 시작",
                Route.PhoneInput to "부모로 시작",
            )
        }

        // ── 인증 ─────────────────────────────────────────────
        composable<Route.PhoneInput> {
            Placeholder(
                "휴대폰 번호 입력", navController,
                Route.PhoneVerify("01000000000") to "인증번호 받기",
            )
        }
        composable<Route.PhoneVerify> { entry ->
            val args = entry.toRoute<Route.PhoneVerify>()
            Placeholder(
                "인증번호 확인\n${args.phoneNumber}", navController,
                Route.Terms to "확인",
            )
        }
        composable<Route.Terms> {
            Placeholder("약관 동의", navController, Route.SignUp to "동의하고 계속")
        }
        composable<Route.SignUp> {
            Placeholder("회원가입", navController, Route.PermissionIntro to "가입 완료")
        }
        composable<Route.Login> {
            Placeholder("로그인", navController, Route.Home to "로그인")
        }

        // ── 권한 · 기기 설정 ──────────────────────────────────
        composable<Route.PermissionIntro> {
            Placeholder("권한 안내", navController, Route.PermissionRequest to "다음")
        }
        composable<Route.PermissionRequest> {
            Placeholder("권한 요청", navController, Route.BatteryOptimization to "다음")
        }
        composable<Route.BatteryOptimization> {
            Placeholder("배터리 최적화 해제", navController, Route.AutoRecordingGuide to "다음")
        }
        composable<Route.AutoRecordingGuide> {
            Placeholder("자동 통화녹음 설정 안내", navController, Route.VoiceIntro to "설정했어요")
        }
        composable<Route.SetupComplete> {
            Placeholder("설정 완료", navController, Route.Home to "시작하기")
        }

        // ── 목소리 등록 ───────────────────────────────────────
        composable<Route.VoiceIntro> {
            Placeholder("목소리 등록 안내", navController, Route.VoiceRecord(1) to "녹음 시작")
        }
        composable<Route.VoiceRecord> { entry ->
            val args = entry.toRoute<Route.VoiceRecord>()
            Placeholder(
                "녹음 ${args.sentenceId}/3", navController,
                Route.VoiceConfirm(args.sentenceId) to "녹음 완료",
            )
        }
        composable<Route.VoiceConfirm> { entry ->
            val args = entry.toRoute<Route.VoiceConfirm>()
            val next = if (args.sentenceId < 3)
                Route.VoiceRecord(args.sentenceId + 1) else Route.VoiceComplete
            Placeholder("녹음 확인 ${args.sentenceId}/3", navController, next to "다음")
        }
        composable<Route.VoiceComplete> {
            Placeholder("등록 완료", navController, Route.FamilyCreate to "가족 연결하기")
        }

        // ── 가족 ─────────────────────────────────────────────
        composable<Route.FamilyCreate> {
            Placeholder("가족 공간 만들기", navController, Route.FamilyInvite to "만들기")
        }
        composable<Route.FamilyInvite> {
            Placeholder("초대 링크 / QR", navController, Route.Home to "완료")
        }
        composable<Route.InvitePreview> {
            Placeholder("초대 미리보기", navController, Route.Home to "수락")
        }
        composable<Route.FamilyList> {
            Placeholder("가족 목록", navController, Route.FamilyMemberDetail(1) to "구성원 보기")
        }
        composable<Route.FamilyMemberDetail> {
            Placeholder("가족 구성원 상세", navController)
        }

        // ── 홈 ───────────────────────────────────────────────
        composable<Route.Home> {
            Placeholder(
                "홈", navController,
                Route.History to "분석 기록",
                Route.FamilyList to "가족",
                Route.Settings to "설정",
                Route.DemoIntro to "데모",
                Route.Analyzing(1) to "[테스트] 분석 화면",
            )
        }
        composable<Route.ProtectionStatus> {
            Placeholder("보호 상태", navController)
        }

        // ── 분석 ─────────────────────────────────────────────
        composable<Route.Analyzing> {
            Placeholder("분석 중…", navController, Route.AnalysisResult(1) to "결과 보기")
        }
        composable<Route.AnalysisResult> {
            Placeholder(
                "분석 결과", navController,
                Route.DangerAlert(1) to "위험 화면 보기",
                Route.History to "기록으로",
            )
        }
        composable<Route.History> {
            Placeholder("분석 기록", navController, Route.AnalysisDetail(1) to "상세 보기")
        }
        composable<Route.AnalysisDetail> {
            Placeholder("분석 상세", navController)
        }

        // ── 위험 대응 ─────────────────────────────────────────
        composable<Route.DangerAlert> {
            Placeholder("위험 경고", navController, Route.ShareWithFamily(1) to "가족에게 알리기")
        }
        composable<Route.ShareWithFamily> {
            Placeholder("가족 공유", navController, Route.SharedNumbers to "공유된 번호")
        }
        composable<Route.SharedNumbers> {
            Placeholder("공유된 위험 번호", navController)
        }

        // ── 데모 ─────────────────────────────────────────────
        composable<Route.DemoIntro> {
            Placeholder("데모 소개", navController, Route.DemoPlay to "시작")
        }
        composable<Route.DemoPlay> {
            Placeholder("real / fake 맞추기", navController, Route.DemoResult to "제출")
        }
        composable<Route.DemoResult> {
            Placeholder("데모 결과", navController, Route.Home to "홈으로")
        }

        // ── 설정 ─────────────────────────────────────────────
        composable<Route.Settings> {
            Placeholder(
                "설정", navController,
                Route.NotificationSettings to "알림 설정",
                Route.AccountSettings to "계정 설정",
            )
        }
        composable<Route.NotificationSettings> {
            Placeholder("알림 설정", navController)
        }
        composable<Route.AccountSettings> {
            Placeholder("계정 설정", navController)
        }
    }
}

/**
 * 임시 화면.
 *
 * 실제 화면이 완성될 때마다 위의 Placeholder 호출을
 * 해당 Route Composable로 교체하면 됩니다.
 */
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
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)

        actions.forEach { (route, label) ->
            Button(onClick = { navController.navigate(route) }) { Text(label) }
        }

        if (navController.previousBackStackEntry != null) {
            Button(onClick = { navController.popBackStack() }) { Text("뒤로") }
        }
    }
}
