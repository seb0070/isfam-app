package com.isfam.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.isfam.core.designsystem.Amber500
import com.isfam.core.designsystem.CautionBadgeBg
import com.isfam.core.designsystem.CautionBadgeFg
import com.isfam.core.designsystem.Danger
import com.isfam.core.designsystem.DangerBadgeBg
import com.isfam.core.designsystem.Honey400
import com.isfam.core.designsystem.Ink
import com.isfam.core.designsystem.InkBody
import com.isfam.core.designsystem.InkFaint
import com.isfam.core.designsystem.InkMuted
import com.isfam.core.designsystem.IsFamScaffold
import com.isfam.core.designsystem.IsFamTheme
import com.isfam.core.designsystem.MatchBarEnd
import com.isfam.core.designsystem.MatchBarStart
import com.isfam.core.designsystem.OutlineWarm
import com.isfam.core.designsystem.RiskLevel
import com.isfam.core.designsystem.RowDivider
import com.isfam.core.designsystem.SafeBadgeBg
import com.isfam.core.designsystem.SafeBadgeFg
import com.isfam.core.designsystem.SegmentDangerLight
import com.isfam.core.designsystem.SegmentInactive
import com.isfam.core.designsystem.SpoofBarEnd
import com.isfam.core.designsystem.SpoofBarStart
import com.isfam.core.designsystem.TrackWarm
import com.isfam.core.designsystem.TrustBarEnd
import com.isfam.core.designsystem.TrustBarStart
import com.isfam.core.designsystem.White

/**
 * 33. 기록 상세
 *
 * UI 키트 실측값
 *   헤더 카드 radius 22 · 아바타 44 원형 · 배지 pill
 *   구간 그래프 height 74 · 막대 gap 2
 *   지표 막대 height 7 radius 5
 *   하단 버튼 2개 나란히 height 54
 *   좌우 여백 22
 *
 * ⚠️ "송금 요구 키워드 포함"은 STT 결과입니다.
 *    현재 서버는 STT 를 쓰지 않으므로 이 항목은 표시되지 않습니다.
 *    문맥 분석은 v2 범위입니다.
 */
data class HistoryDetailUiState(
    val analysisId: Long,
    val caller: String,
    val dateLabel: String,
    val methodLabel: String,
    val durationLabel: String,
    val totalSeconds: Int,
    val riskLevel: RiskLevel,
    /** 구간별 위험도. 0.0~1.0 값과 등급 */
    val segments: List<RiskSegment>,
    val findings: List<Finding>,
    val matchScore: Int,
    val spoofScore: Int,
    val confidenceScore: Int,
    val handledLabel: String?,
    val sharedLabel: String?,
)

data class RiskSegment(val height: Float, val level: RiskLevel)

data class Finding(
    val timeRange: String,
    val description: String,
    val level: RiskLevel,
)

object FakeDetailData {
    private fun segments(): List<RiskSegment> {
        val heights = listOf(
            .26f, .44f, .70f, .38f, .86f, .58f, .96f, .42f, .78f,
            .64f, .92f, .50f, .88f, .36f, .74f, .60f, .98f, .46f, .82f, .54f,
            .90f, .40f, .76f, .62f, .94f, .48f,
        )
        return heights.mapIndexed { i, h ->
            RiskSegment(h, if (i in 9..19) RiskLevel.DANGER else RiskLevel.CAUTION)
        } + List(18) { RiskSegment(0.3f + (it % 5) * 0.13f, RiskLevel.INSUFFICIENT) }
    }

    val danger = HistoryDetailUiState(
        analysisId = 1,
        caller = "010-4482-9917",
        dateLabel = "오늘 오후 12:03",
        methodLabel = "통화 후 자동 분석",
        durationLabel = "01:07",
        totalSeconds = 67,
        riskLevel = RiskLevel.DANGER,
        segments = segments(),
        findings = listOf(
            Finding("00:03 - 00:11", "억양 패턴 불일치", RiskLevel.CAUTION),
            Finding("00:18 - 00:29", "합성음 스펙트럼 특징 감지", RiskLevel.DANGER),
        ),
        matchScore = 41,
        spoofScore = 92,
        confidenceScore = 97,
        handledLabel = "차단 · 신고 완료",
        sharedLabel = "3명에게 알림 발송",
    )
}

@Composable
fun HistoryDetailRoute(
    analysisId: Long,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onReportGuide: () -> Unit,
    onHome: () -> Unit,
) {
    // TODO: Repository 연결 시 교체
    val state = FakeDetailData.danger.copy(analysisId = analysisId)

    HistoryDetailScreen(
        state = state,
        onBack = onBack,
        onDelete = onDelete,
        onReportGuide = onReportGuide,
        onHome = onHome,
    )
}

@Composable
fun HistoryDetailScreen(
    state: HistoryDetailUiState,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onReportGuide: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IsFamScaffold(
        modifier = modifier,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 22.dp, top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "←",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 22.sp),
                    color = Ink,
                    modifier = Modifier.clickable(onClick = onBack),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "기록 상세",
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 17.sp),
                        color = Ink,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        state.dateLabel,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = InkFaint,
                    )
                }
                Text(
                    "삭제",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    color = InkFaint,
                    modifier = Modifier.clickable(onClick = onDelete),
                )
            }
        },
        bottomBar = {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlineAction("경찰청 신고 안내", onReportGuide, Modifier.weight(1f))
                GradientAction("홈으로", onHome, Modifier.weight(1f))
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            CallerCard(state)

            Spacer(Modifier.height(11.dp))
            SegmentGraphCard(state)

            Spacer(Modifier.height(11.dp))
            ScoreCard(state)

            if (state.handledLabel != null || state.sharedLabel != null) {
                Spacer(Modifier.height(11.dp))
                HandledCard(state)
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

// ── 발신자 카드 ───────────────────────────────────────────────

@Composable
private fun CallerCard(state: HistoryDetailUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(5.dp, RoundedCornerShape(22.dp), clip = false)
            .clip(RoundedCornerShape(22.dp))
            .background(White)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(Color(0xFFFBDEDC), Color(0xFFF2B7B3)))
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "!",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 17.sp),
                color = Color(0xFFB33A35),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                state.caller,
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 17.sp),
                color = Ink,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                "${state.methodLabel} · ${state.durationLabel}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = InkMuted,
            )
        }
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(Color(0xFFE4776D), Color(0xFFC93B35)))
                )
                .padding(horizontal = 12.dp, vertical = 7.dp),
        ) {
            Text(
                state.riskLevel.label,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = White,
            )
        }
    }
}

// ── 구간별 위험도 ─────────────────────────────────────────────

@Composable
private fun SegmentGraphCard(state: HistoryDetailUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(5.dp, RoundedCornerShape(24.dp), clip = false)
            .clip(RoundedCornerShape(24.dp))
            .background(White)
            .padding(18.dp),
    ) {
        Text(
            "음성 구간별 위험도",
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.5.sp),
            color = Ink,
        )

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth().height(74.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            state.segments.forEach { seg ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(seg.height)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            when (seg.level) {
                                RiskLevel.DANGER -> Danger
                                RiskLevel.CAUTION -> SegmentDangerLight
                                else -> SegmentInactive
                            }
                        ),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "00:00",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = InkFaint,
            )
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(RowDivider)
                    .padding(horizontal = 16.dp, vertical = 7.dp),
            ) {
                Text(
                    "▶ 재생",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = Ink,
                )
            }
            Text(
                state.durationLabel,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = InkFaint,
            )
        }

        if (state.findings.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                state.findings.forEach { FindingRow(it) }
            }
        }
    }
}

@Composable
private fun FindingRow(finding: Finding) {
    val (fg, bg) = when (finding.level) {
        RiskLevel.DANGER -> Color(0xFFC93B35) to DangerBadgeBg
        else -> CautionBadgeFg to Color(0xFFFFF4E0)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(7.dp))
                .background(bg)
                .padding(horizontal = 9.dp, vertical = 5.dp),
        ) {
            Text(
                finding.timeRange,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = fg,
            )
        }
        Text(
            finding.description,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
            color = InkBody,
        )
    }
}

// ── 지표 ──────────────────────────────────────────────────────

@Composable
private fun ScoreCard(state: HistoryDetailUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(5.dp, RoundedCornerShape(24.dp), clip = false)
            .clip(RoundedCornerShape(24.dp))
            .background(White)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        ScoreBar("가족 목소리 일치도", state.matchScore, listOf(MatchBarStart, MatchBarEnd))
        ScoreBar("AI 합성 확률", state.spoofScore, listOf(SpoofBarStart, SpoofBarEnd))
        ScoreBar("분석 신뢰도", state.confidenceScore, listOf(TrustBarStart, TrustBarEnd))
    }
}

@Composable
private fun ScoreBar(label: String, score: Int, colors: List<Color>) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = InkBody,
            )
            Text(
                "$score%",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 13.5.sp),
                color = Ink,
            )
        }
        Spacer(Modifier.height(7.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(TrackWarm),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(score / 100f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(5.dp))
                    .background(Brush.horizontalGradient(colors)),
            )
        }
    }
}

// ── 처리 내역 ─────────────────────────────────────────────────

@Composable
private fun HandledCard(state: HistoryDetailUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(5.dp, RoundedCornerShape(22.dp), clip = false)
            .clip(RoundedCornerShape(22.dp))
            .background(White),
    ) {
        state.handledLabel?.let {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "처리 내역",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = Ink,
                )
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(SafeBadgeBg)
                        .padding(horizontal = 11.dp, vertical = 6.dp),
                ) {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = SafeBadgeFg,
                    )
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(RowDivider))
        }

        state.sharedLabel?.let {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "가족 공유",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = Ink,
                )
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                    color = InkFaint,
                )
            }
        }
    }
}

// ── 하단 버튼 ─────────────────────────────────────────────────

@Composable
private fun OutlineAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(54.dp)
            .shadow(5.dp, RoundedCornerShape(18.dp), clip = false)
            .clip(RoundedCornerShape(18.dp))
            .background(White)
            .border(1.5.dp, OutlineWarm, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp), color = Ink)
    }
}

@Composable
private fun GradientAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(54.dp)
            .shadow(10.dp, RoundedCornerShape(18.dp), clip = false)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(Honey400, Amber500)))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp), color = White)
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 850)
@Composable
private fun HistoryDetailPreview() = IsFamTheme {
    HistoryDetailScreen(
        state = FakeDetailData.danger,
        onBack = {}, onDelete = {}, onReportGuide = {}, onHome = {},
    )
}
