package com.isfam.feature.result

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.isfam.core.designsystem.DangerCtaRed
import com.isfam.core.designsystem.Ink
import com.isfam.core.designsystem.InkMuted
import com.isfam.core.designsystem.Mascot
import com.isfam.core.designsystem.MascotImage
import com.isfam.core.designsystem.SafeCtaStart
import com.isfam.core.designsystem.White

/**
 * 28 · 29 · 30. 분석 결과
 *
 * 세 화면이 아니라 한 화면입니다. verdict 에 따라 테마 전체가 바뀝니다.
 *
 * UI 키트 실측값
 *   안전   배경 gradient(180deg #F7FCF9→#E9F4EE 62%→#E2F0E8) · 링 186/146
 *   확인   배경 gradient(#FFFCF4→#FDF3E1 62%→#FAEBD5)       · 링 186/146
 *   위험   배경 #2A1112 (다크)                               · 링 168/132
 *
 *   점수 800 48(위험 44) · 배지 pill 700 12
 *   제목 800 26~27/1.4 · 본문 500 14/1.65
 *   상세 카드 radius 26(위험 24) · padding 20 · gap 15
 */
@Composable
fun AnalysisResultRoute(
    analysisId: Long,
    onClose: () -> Unit,
    onShareToFamily: (Long) -> Unit,
    onBlockAndReport: (String) -> Unit,
    onSeeDetail: (Long) -> Unit,
) {
    // TODO: Repository 연결. 지금은 id 로 목 데이터를 고릅니다.
    val state = when (analysisId % 3L) {
        1L -> FakeResultData.safe
        2L -> FakeResultData.caution
        else -> FakeResultData.danger
    }

    AnalysisResultScreen(
        state = state,
        onClose = onClose,
        onShareToFamily = { onShareToFamily(state.analysisId) },
        onBlockAndReport = { state.phoneNumber?.let(onBlockAndReport) },
        onSeeDetail = { onSeeDetail(state.analysisId) },
    )
}

@Composable
fun AnalysisResultScreen(
    state: AnalysisResultUiState,
    onClose: () -> Unit,
    onShareToFamily: () -> Unit,
    onBlockAndReport: () -> Unit,
    onSeeDetail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = ResultTheme.of(state.verdict)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(theme.backgroundColors)),
    ) {
        // 상단 바
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 26.dp, end = 26.dp, top = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "✕",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 22.sp),
                color = theme.topIcon,
                modifier = Modifier.clickable(onClick = onClose),
            )
            if (state.verdict != Verdict.DANGER) {
                Text(
                    "공유",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = InkMuted,
                    modifier = Modifier.clickable(onClick = onShareToFamily),
                )
            }
        }

        // 점수 링 + 판정
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp)) {
            // 마스코트는 우상단에 겹칩니다
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp)
                    .size(92.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(theme.mascotBg)
                    .padding(6.dp),
                contentAlignment = Alignment.Center,
            ) {
                MascotImage(
                    mascot = theme.mascot,
                    size = 80.dp,
                    cornerRadius = 24.dp,
                    background = Color.Transparent,
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ScoreRing(
                    score = state.matchScore,
                    theme = theme,
                )

                Spacer(Modifier.height(if (state.verdict == Verdict.CAUTION) 16.dp else 22.dp))

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Brush.linearGradient(theme.badgeColors))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        state.badgeText,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = theme.badgeText,
                    )
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    state.title,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = if (state.verdict == Verdict.DANGER) 27.sp else 26.sp,
                        lineHeight = 37.sp,
                    ),
                    color = theme.titleText,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    state.description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp, lineHeight = 23.sp,
                    ),
                    color = theme.bodyText,
                    textAlign = TextAlign.Center,
                )
            }
        }

        // 상세 카드
        Box(modifier = Modifier.weight(1f).padding(start = 26.dp, end = 26.dp, top = 24.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(if (state.verdict == Verdict.DANGER) 24.dp else 26.dp))
                    .background(theme.cardBackground)
                    .then(
                        theme.cardBorder?.let {
                            Modifier.border(
                                1.dp, it,
                                RoundedCornerShape(if (state.verdict == Verdict.DANGER) 24.dp else 26.dp)
                            )
                        } ?: Modifier
                    )
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(15.dp),
            ) {
                state.details.forEachIndexed { index, row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            row.label,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            color = theme.rowLabel,
                        )
                        Text(
                            row.value,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                            color = if (row.emphasized) theme.scoreText else theme.rowValue,
                        )
                    }
                    if (index < state.details.lastIndex) {
                        Box(Modifier.fillMaxWidth().height(1.dp).background(theme.rowDivider))
                    }
                }
            }
        }

        // 하단 액션
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 26.dp, end = 26.dp, bottom = 34.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            when (state.verdict) {
                Verdict.DANGER -> {
                    // 차단은 번호를 아는 경우에만 노출합니다.
                    // 파일명이 연락처 이름이면 번호를 알 수 없고,
                    // 이때는 진짜 가족 번호일 수 있어 차단하면 안 됩니다.
                    if (state.phoneNumber != null) {
                        FilledAction(
                            text = "번호 차단하고 신고하기",
                            background = DangerCtaRed,
                            contentColor = White,
                            onClick = onBlockAndReport,
                        )
                    }
                    FilledAction(
                        text = "가족에게 위험 알리기",
                        background = White.copy(alpha = 0.1f),
                        contentColor = White,
                        height = 54,
                        onClick = onShareToFamily,
                    )
                }

                Verdict.CAUTION, Verdict.INSUFFICIENT -> {
                    FilledAction(
                        text = "직접 전화해서 확인하기",
                        background = Ink,
                        contentColor = White,
                        onClick = onClose,
                    )
                    FilledAction(
                        text = "가족에게 공유하기",
                        background = White.copy(alpha = 0.7f),
                        contentColor = Ink,
                        height = 54,
                        onClick = onShareToFamily,
                    )
                }

                Verdict.SAFE -> {
                    FilledAction(
                        text = "확인",
                        background = SafeCtaStart,
                        contentColor = White,
                        onClick = onClose,
                    )
                    Text(
                        "기록에 저장됨 · 자세히 보기",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                        color = theme.rowLabel,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().clickable(onClick = onSeeDetail),
                    )
                }
            }
        }
    }
}

// ── 점수 링 ───────────────────────────────────────────────────

/**
 * 원형 점수 게이지.
 *
 * CSS 의 conic-gradient 를 Compose 에서는 sweepGradient + drawArc 로 그립니다.
 * 시작 각도를 -90도로 두어 12시 방향에서 시작합니다.
 */
@Composable
private fun ScoreRing(
    score: Int,
    theme: ResultTheme,
    modifier: Modifier = Modifier,
) {
    val animated by animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = tween(900),
        label = "scoreRing",
    )
    val outer = theme.ringOuterSize.dp
    val inner = theme.ringInnerSize.dp
    val strokeWidth = (theme.ringOuterSize - theme.ringInnerSize) / 2f

    Box(
        modifier = modifier.size(outer),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(outer)) {
            val stroke = strokeWidth.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)

            // 미달 구간 (연한 배경)
            drawArc(
                color = theme.ringColors.last().copy(alpha = 0.12f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )

            // 점수 구간
            drawArc(
                brush = Brush.sweepGradient(theme.ringColors),
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }

        Box(
            modifier = Modifier
                .size(inner)
                .clip(CircleShape)
                .background(Brush.radialGradient(theme.innerCircle)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "$score",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = if (theme.ringOuterSize < 180) 44.sp else 48.sp,
                    ),
                    color = theme.scoreText,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "일치 점수",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    color = theme.scoreLabel,
                )
            }
        }
    }
}

// ── 하단 버튼 ─────────────────────────────────────────────────

@Composable
private fun FilledAction(
    text: String,
    background: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Int = 56,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = if (height < 56) 15.sp else 16.sp,
            ),
            color = contentColor,
        )
    }
}

// ── Preview ───────────────────────────────────────────────────

@Preview(name = "28 안전", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ResultSafePreview() =
    AnalysisResultScreen(FakeResultData.safe, {}, {}, {}, {})

@Preview(name = "29 확인 필요", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ResultCautionPreview() =
    AnalysisResultScreen(FakeResultData.caution, {}, {}, {}, {})

@Preview(name = "30 위험", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ResultDangerPreview() =
    AnalysisResultScreen(FakeResultData.danger, {}, {}, {}, {})
