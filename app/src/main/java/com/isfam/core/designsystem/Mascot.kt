package com.isfam.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 미어켓 마스코트 슬롯.
 *
 * UI 키트 "마스코트 가이드라인 · 미어켓" 규칙을 코드로 고정했습니다.
 *   상황별 표정 6종
 *   크기 — 스플래시 200 / 온보딩 340 / 화면 제목 옆 84~88 / 카드 내부 60 / 결과 링 옆 92
 *   항상 라운드 컨테이너(radius 20~64) 안에 여백 6~10dp
 *
 * ── 실제 이미지 넣는 법 ──────────────────────────────────────
 * 1. app/src/main/res/drawable/ 에 아래 이름으로 넣습니다
 *      mascot_watching.png     (또는 .xml 벡터)
 *      mascot_listening.png
 *      mascot_analyzing.png
 *      mascot_safe.png
 *      mascot_caution.png
 *      mascot_danger.png
 *    ⚠️ 파일명은 소문자·숫자·언더스코어만 가능합니다
 *
 * 2. 아래 MascotImage 의 주석을 해제하고 Placeholder 분기를 지웁니다
 *
 * 지금은 이모지 자리표시자로 동작합니다. 레이아웃이 이미 잡혀 있어서
 * 이미지만 넣으면 화면 코드는 한 줄도 안 바꿔도 됩니다.
 */
enum class Mascot(
    val emoji: String,
    val resourceName: String,
    val description: String,
) {
    /** 홈 — 지켜보는 중 */
    Watching("👀", "mascot_watching", "지켜보는 미어켓"),

    /** 통화 — 듣는 중 */
    Listening("👂", "mascot_listening", "귀 기울이는 미어켓"),

    /** 백그라운드 — 분석 중 */
    Analyzing("🔎", "mascot_analyzing", "분석하는 미어켓"),

    /** 결과 — 안전 */
    Safe("😊", "mascot_safe", "안심하는 미어켓"),

    /** 결과 — 확인 필요 */
    Caution("🤔", "mascot_caution", "갸웃하는 미어켓"),

    /** 결과 — 위험 */
    Danger("😰", "mascot_danger", "놀란 미어켓"),
}

/** UI 키트에 정의된 배치 크기 */
object MascotSize {
    val Splash: Dp = 200.dp
    val Onboarding: Dp = 340.dp
    val Headline: Dp = 86.dp
    val Card: Dp = 60.dp
    val ResultRing: Dp = 92.dp
}

@Composable
fun MascotImage(
    mascot: Mascot,
    size: Dp,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = (size.value * 0.18f).dp.coerceIn(20.dp, 64.dp),
    background: androidx.compose.ui.graphics.Color = IllustBgStart,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(background, RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center,
    ) {
        // 실제 에셋을 넣은 뒤 아래 주석을 해제하고 Text 블록을 지우세요.
        //
        // val context = LocalContext.current
        // val resId = remember(mascot) {
        //     context.resources.getIdentifier(
        //         mascot.resourceName, "drawable", context.packageName
        //     )
        // }
        // Image(
        //     painter = painterResource(resId),
        //     contentDescription = mascot.description,
        //     modifier = Modifier.fillMaxSize().padding(size * 0.04f),
        // )

        Text(
            text = mascot.emoji,
            style = MaterialTheme.typography.displayMedium,
        )
    }
}

private fun Dp.coerceIn(min: Dp, max: Dp): Dp = when {
    this < min -> min
    this > max -> max
    else -> this
}