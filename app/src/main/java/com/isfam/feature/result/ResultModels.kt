package com.isfam.feature.result

import androidx.compose.ui.graphics.Color
import com.isfam.core.designsystem.CautionBadgeStart
import com.isfam.core.designsystem.CautionBgBottom
import com.isfam.core.designsystem.CautionBgMid
import com.isfam.core.designsystem.CautionBgTop
import com.isfam.core.designsystem.CautionBodyText
import com.isfam.core.designsystem.CautionMascotBg
import com.isfam.core.designsystem.CautionRingDeep
import com.isfam.core.designsystem.CautionRingLight
import com.isfam.core.designsystem.CautionRingMid
import com.isfam.core.designsystem.CautionRowDivider
import com.isfam.core.designsystem.CautionRowLabel
import com.isfam.core.designsystem.CautionScoreLabel
import com.isfam.core.designsystem.CautionScoreText
import com.isfam.core.designsystem.DangerBgDark
import com.isfam.core.designsystem.DangerRingDeep
import com.isfam.core.designsystem.DangerRingLight
import com.isfam.core.designsystem.DangerScoreText
import com.isfam.core.designsystem.Ink
import com.isfam.core.designsystem.Mascot
import com.isfam.core.designsystem.SafeBadgeStart
import com.isfam.core.designsystem.SafeBgBottom
import com.isfam.core.designsystem.SafeBgMid
import com.isfam.core.designsystem.SafeBgTop
import com.isfam.core.designsystem.SafeBodyText
import com.isfam.core.designsystem.SafeMascotBg
import com.isfam.core.designsystem.SafeRingDeep
import com.isfam.core.designsystem.SafeRingLight
import com.isfam.core.designsystem.SafeRingMid
import com.isfam.core.designsystem.SafeRowDivider
import com.isfam.core.designsystem.SafeRowLabel
import com.isfam.core.designsystem.SafeScoreLabel
import com.isfam.core.designsystem.SafeScoreText
import com.isfam.core.designsystem.White

/**
 * 28·29·30 분석 결과.
 *
 * 세 화면이 아니라 한 화면입니다. verdict 값에 따라 테마가 통째로 바뀝니다.
 */
enum class Verdict { SAFE, CAUTION, DANGER, INSUFFICIENT }

data class AnalysisResultUiState(
    val analysisId: Long,
    val verdict: Verdict,
    /** 0~100. 서버 similarity_score(0.0~1.0)를 변환한 값 */
    val matchScore: Int,
    val badgeText: String,
    val title: String,
    val description: String,
    val details: List<DetailRow>,
    /** 차단 버튼 노출 조건. 파일명이 번호일 때만 true */
    val phoneNumber: String?,
    val savedToHistory: Boolean = true,
)

data class DetailRow(
    val label: String,
    val value: String,
    /** 값을 강조색으로 표시할지 */
    val emphasized: Boolean = false,
)

/**
 * 등급별 테마.
 * 배경·링·카드·텍스트 색이 전부 다릅니다.
 */
data class ResultTheme(
    val backgroundColors: List<Color>,
    val ringColors: List<Color>,
    /** 링 안쪽 원 배경 */
    val innerCircle: List<Color>,
    val scoreText: Color,
    val scoreLabel: Color,
    val badgeColors: List<Color>,
    val badgeText: Color,
    val titleText: Color,
    val bodyText: Color,
    val cardBackground: Color,
    val cardBorder: Color?,
    val rowLabel: Color,
    val rowValue: Color,
    val rowDivider: Color,
    val mascotBg: Color,
    val mascot: Mascot,
    val topIcon: Color,
    val ringOuterSize: Int,
    val ringInnerSize: Int,
) {
    companion object {
        fun of(verdict: Verdict): ResultTheme = when (verdict) {
            Verdict.SAFE -> ResultTheme(
                backgroundColors = listOf(SafeBgTop, SafeBgMid, SafeBgBottom),
                ringColors = listOf(SafeRingLight, SafeRingMid, SafeRingDeep),
                innerCircle = listOf(White, Color(0xFFEFF8F2)),
                scoreText = SafeScoreText,
                scoreLabel = SafeScoreLabel,
                badgeColors = listOf(SafeBadgeStart, SafeRingDeep),
                badgeText = White,
                titleText = Ink,
                bodyText = SafeBodyText,
                cardBackground = White.copy(alpha = 0.88f),
                cardBorder = null,
                rowLabel = SafeRowLabel,
                rowValue = Ink,
                rowDivider = SafeRowDivider,
                mascotBg = SafeMascotBg,
                mascot = Mascot.Safe,
                topIcon = Ink,
                ringOuterSize = 186,
                ringInnerSize = 146,
            )

            Verdict.CAUTION, Verdict.INSUFFICIENT -> ResultTheme(
                backgroundColors = listOf(CautionBgTop, CautionBgMid, CautionBgBottom),
                ringColors = listOf(CautionRingLight, CautionRingMid, CautionRingDeep),
                innerCircle = listOf(White, Color(0xFFFDF6EA)),
                scoreText = CautionScoreText,
                scoreLabel = CautionScoreLabel,
                badgeColors = listOf(CautionBadgeStart, CautionRingDeep),
                badgeText = White,
                titleText = Ink,
                bodyText = CautionBodyText,
                cardBackground = White.copy(alpha = 0.88f),
                cardBorder = null,
                rowLabel = CautionRowLabel,
                rowValue = Ink,
                rowDivider = CautionRowDivider,
                mascotBg = CautionMascotBg,
                mascot = Mascot.Caution,
                topIcon = Ink,
                ringOuterSize = 186,
                ringInnerSize = 146,
            )

            Verdict.DANGER -> ResultTheme(
                backgroundColors = listOf(DangerBgDark, DangerBgDark),
                ringColors = listOf(DangerRingLight, DangerRingDeep),
                innerCircle = listOf(DangerBgDark, DangerBgDark),
                scoreText = DangerScoreText,
                scoreLabel = White.copy(alpha = 0.5f),
                badgeColors = listOf(DangerScoreText, DangerScoreText),
                badgeText = DangerBgDark,
                titleText = White,
                bodyText = White.copy(alpha = 0.62f),
                cardBackground = White.copy(alpha = 0.07f),
                cardBorder = White.copy(alpha = 0.12f),
                rowLabel = White.copy(alpha = 0.6f),
                rowValue = White,
                rowDivider = White.copy(alpha = 0.1f),
                mascotBg = White.copy(alpha = 0.1f),
                mascot = Mascot.Danger,
                topIcon = White,
                ringOuterSize = 168,
                ringInnerSize = 132,
            )
        }
    }
}

/** 서버 연동 전 화면을 채우는 데이터 */
object FakeResultData {
    val safe = AnalysisResultUiState(
        analysisId = 1,
        verdict = Verdict.SAFE,
        matchScore = 96,
        badgeText = "안전 · 가족 목소리",
        title = "아버지 목소리와\n일치해요",
        description = "AI 합성 흔적이 발견되지 않았습니다",
        details = listOf(
            DetailRow("성문 유사도", "96% · 매우 높음"),
            DetailRow("합성 음성 확률", "2%", emphasized = true),
            DetailRow("배경 잡음", "낮음"),
            DetailRow("분석 길이", "42초"),
        ),
        phoneNumber = null,
    )

    val caution = AnalysisResultUiState(
        analysisId = 2,
        verdict = Verdict.CAUTION,
        matchScore = 62,
        badgeText = "확인 필요 · 애매해요",
        title = "가족 목소리와\n비슷하지만 확실하지 않아요",
        description = "직접 다시 전화해서 확인해 보세요",
        details = listOf(
            DetailRow("성문 유사도", "62% · 보통"),
            DetailRow("합성 음성 확률", "31%", emphasized = true),
            DetailRow("배경 잡음", "높음"),
            DetailRow("분석 길이", "26초"),
        ),
        phoneNumber = null,
    )

    val danger = AnalysisResultUiState(
        analysisId = 3,
        verdict = Verdict.DANGER,
        matchScore = 18,
        badgeText = "위험 · 즉시 중단",
        title = "AI 합성 음성으로\n의심됩니다",
        description = "가족의 목소리와 일치하지 않아요.\n돈·인증번호 요구에 절대 응하지 마세요.",
        details = listOf(
            DetailRow("성문 유사도", "18% · 매우 낮음", emphasized = true),
            DetailRow("합성 음성 확률", "94%", emphasized = true),
            DetailRow("탐지된 흔적", "주파수 반복 패턴"),
            DetailRow("발신 번호", "010 5521 8842"),
        ),
        phoneNumber = "01055218842",
    )
}
