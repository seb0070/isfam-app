package com.isfam.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue

/*
 * ══════════════════════════════════════════════════════════════
 *  화면 하나의 표준 구성.
 *  33개 화면을 이 형태로 찍어내면 됩니다.
 *
 *  ① UiState      화면이 가질 수 있는 상태를 전부 나열
 *  ② ViewModel    상태를 만들고 이벤트를 처리
 *  ③ Route        ViewModel과 Screen을 연결 (상태 있음)
 *  ④ Screen       상태를 받아 그리기만 함 (상태 없음 → Preview 가능)
 * ══════════════════════════════════════════════════════════════
 */

// ─── ① UiState ────────────────────────────────────────────────
//
// sealed interface 로 정의하면 "로딩 중인데 데이터도 있는" 같은
// 불가능한 상태가 아예 만들어지지 않습니다.

sealed interface HistoryUiState {
    data object Loading : HistoryUiState
    data object Empty : HistoryUiState
    data class Success(val items: List<AnalysisItem>) : HistoryUiState
    data class Error(val message: String) : HistoryUiState
}

data class AnalysisItem(
    val id: Int,
    val claimedIdentity: String,   // 연락처명 또는 번호
    val riskLevel: RiskLevel,
    val analyzedAt: String,
    val durationSec: Int,
)

enum class RiskLevel { SAFE, CAUTION, DANGER, INSUFFICIENT }

// ─── ② ViewModel ──────────────────────────────────────────────
//
// Repository 를 생성자로 받습니다.
// 지금은 FakeRepository, 서버가 준비되면 RealRepository 로 교체.
// 화면 코드는 한 줄도 바뀌지 않습니다.

class HistoryViewModel(
    private val repository: HistoryRepository = FakeHistoryRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.update { HistoryUiState.Loading }
            runCatching { repository.getAnalyses() }
                .onSuccess { items ->
                    _uiState.update {
                        if (items.isEmpty()) HistoryUiState.Empty
                        else HistoryUiState.Success(items)
                    }
                }
                .onFailure { e ->
                    _uiState.update { HistoryUiState.Error(e.message ?: "불러오지 못했습니다") }
                }
        }
    }
}

// ─── Repository ───────────────────────────────────────────────

interface HistoryRepository {
    suspend fun getAnalyses(): List<AnalysisItem>
}

/** 3일 스프린트 동안 화면을 채우는 가짜 데이터 */
class FakeHistoryRepository : HistoryRepository {
    override suspend fun getAnalyses(): List<AnalysisItem> = listOf(
        AnalysisItem(1, "큰딸", RiskLevel.SAFE, "오늘 14:22", 32),
        AnalysisItem(2, "010-1234-5678", RiskLevel.DANGER, "오늘 11:05", 88),
        AnalysisItem(3, "막내딸", RiskLevel.CAUTION, "어제 19:40", 12),
    )
}

// ─── ③ Route — ViewModel 연결 ─────────────────────────────────

@Composable
fun HistoryRoute(
    onItemClick: (Int) -> Unit,
    viewModel: HistoryViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryScreen(uiState = uiState, onItemClick = onItemClick)
}

// ─── ④ Screen — 상태 없음 ─────────────────────────────────────
//
// ViewModel 을 모르기 때문에 Preview 로 즉시 확인할 수 있습니다.
// 33개 화면을 3일 안에 만들려면 이 점이 결정적입니다.
// 매번 앱을 빌드해서 확인할 시간이 없습니다.

@Composable
fun HistoryScreen(
    uiState: HistoryUiState,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            HistoryUiState.Loading ->
                CircularProgressIndicator(Modifier.align(Alignment.Center))

            HistoryUiState.Empty ->
                Text("아직 분석 기록이 없어요", Modifier.align(Alignment.Center))

            is HistoryUiState.Error ->
                Text(uiState.message, Modifier.align(Alignment.Center))

            is HistoryUiState.Success ->
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(uiState.items, key = { it.id }) { item ->
                        AnalysisCard(item = item, onClick = { onItemClick(item.id) })
                    }
                }
        }
    }
}

@Composable
private fun AnalysisCard(item: AnalysisItem, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(item.claimedIdentity, style = MaterialTheme.typography.titleMedium)
            Text(
                "${item.analyzedAt} · ${item.durationSec}초 · ${item.riskLevel.label()}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun RiskLevel.label() = when (this) {
    RiskLevel.SAFE -> "안전"
    RiskLevel.CAUTION -> "확인 필요"
    RiskLevel.DANGER -> "위험"
    RiskLevel.INSUFFICIENT -> "판정 보류"
}

// ─── Preview ──────────────────────────────────────────────────
//
// 상태별로 하나씩 만들어 두면 앱을 실행하지 않고
// Android Studio 안에서 바로 확인할 수 있습니다.

@Preview(showBackground = true)
@Composable
private fun HistoryScreenSuccessPreview() {
    HistoryScreen(
        uiState = HistoryUiState.Success(FakeHistoryRepository().let {
            listOf(
                AnalysisItem(1, "큰딸", RiskLevel.SAFE, "오늘 14:22", 32),
                AnalysisItem(2, "010-1234-5678", RiskLevel.DANGER, "오늘 11:05", 88),
            )
        }),
        onItemClick = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun HistoryScreenEmptyPreview() {
    HistoryScreen(uiState = HistoryUiState.Empty, onItemClick = {})
}
