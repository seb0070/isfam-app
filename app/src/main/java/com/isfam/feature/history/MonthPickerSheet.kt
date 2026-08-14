package com.isfam.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.isfam.core.designsystem.Amber500
import com.isfam.core.designsystem.Ink
import com.isfam.core.designsystem.InkMuted
import com.isfam.core.designsystem.IsFamTheme
import com.isfam.core.designsystem.RowDivider
import com.isfam.core.designsystem.Tint50
import com.isfam.core.designsystem.White
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 분석 기록의 월 선택 시트.
 *
 * 기획서에는 "7월 ▾" 드롭다운만 있고 열린 상태 화면이 없어
 * 바텀시트로 구현했습니다. 목록·선택 패턴은 20·22번 시트와 같습니다.
 */
data class MonthOption(
    val yearMonth: YearMonth,
    val count: Int,
) {
    val label: String
        get() = yearMonth.format(DateTimeFormatter.ofPattern("yyyy년 M월", Locale.KOREA))

    /** 상단에 짧게 표시할 라벨 — "7월" */
    val shortLabel: String
        get() = yearMonth.format(DateTimeFormatter.ofPattern("M월", Locale.KOREA))
}

/** 최근 N개월 목록. 서버 연동 전 임시 생성기입니다. */
fun recentMonths(
    from: YearMonth = YearMonth.now(),
    count: Int = 6,
    counts: Map<YearMonth, Int> = emptyMap(),
): List<MonthOption> = (0 until count).map { offset ->
    val ym = from.minusMonths(offset.toLong())
    MonthOption(ym, counts[ym] ?: 0)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthPickerSheet(
    options: List<MonthOption>,
    selected: YearMonth,
    onSelect: (YearMonth) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = White,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
    ) {
        MonthPickerContent(options, selected, onSelect)
    }
}

@Composable
fun MonthPickerContent(
    options: List<MonthOption>,
    selected: YearMonth,
    onSelect: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, top = 4.dp, bottom = 32.dp),
    ) {
        Text(
            "기간 선택",
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 19.sp),
            color = Ink,
        )

        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(White),
        ) {
            options.forEachIndexed { index, option ->
                MonthRow(
                    option = option,
                    selected = option.yearMonth == selected,
                    onClick = { onSelect(option.yearMonth) },
                )
                if (index < options.lastIndex) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(RowDivider))
                }
            }
        }
    }
}

@Composable
private fun MonthRow(
    option: MonthOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Tint50 else White)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            option.label,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
            color = if (selected) Amber500 else Ink,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "${option.count}건",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                color = InkMuted,
            )
            if (selected) {
                Text(
                    "✓",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                    color = Amber500,
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 520)
@Composable
private fun MonthPickerPreview() = IsFamTheme {
    val now = YearMonth.of(2026, 7)
    MonthPickerContent(
        options = listOf(
            MonthOption(now, 32),
            MonthOption(now.minusMonths(1), 28),
            MonthOption(now.minusMonths(2), 41),
            MonthOption(now.minusMonths(3), 19),
            MonthOption(now.minusMonths(4), 24),
            MonthOption(now.minusMonths(5), 30),
        ),
        selected = now,
        onSelect = {},
    )
}
