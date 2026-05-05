package pro.xservis.client.ui.screens.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import pro.xservis.client.ui.theme.BgBlack
import pro.xservis.client.ui.theme.BgCharcoal
import pro.xservis.client.ui.theme.DangerRed
import pro.xservis.client.ui.theme.SuccessGreen
import pro.xservis.client.ui.theme.SurfaceGrey
import pro.xservis.client.ui.theme.SurfaceGreyElev
import pro.xservis.client.ui.theme.Teal
import pro.xservis.client.ui.theme.TealDim
import pro.xservis.client.ui.theme.TextPrimary
import pro.xservis.client.ui.theme.TextSecondary
import pro.xservis.client.ui.theme.WarningAmber

@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(BgBlack, BgCharcoal, BgBlack),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))
            HeaderRow(state)
            Spacer(Modifier.height(28.dp))
            ConnectButton(
                state = state.connectionState,
                onToggle = { scope.launch { viewModel.toggleConnection() } },
            )
            Spacer(Modifier.height(20.dp))
            StatusLabel(state.connectionState)
            Spacer(Modifier.height(28.dp))
            ScanCard(
                scanState = state.scanState,
                lastResult = state.lastScanResult,
                onScan = { scope.launch { viewModel.scanNetwork() } },
            )
            Spacer(Modifier.height(16.dp))
            QuickStats(state)
        }
    }
}

@Composable
private fun HeaderRow(state: HomeUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                "xservis",
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp,
            )
            Text(
                state.subtitle,
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        IconButton(onClick = { /* notifications */ }) {
            Icon(Icons.Outlined.Bolt, contentDescription = null, tint = Teal)
        }
    }
}

@Composable
private fun ConnectButton(state: ConnectionState, onToggle: () -> Unit) {
    val ringColor = when (state) {
        ConnectionState.Disconnected -> SurfaceGreyElev
        ConnectionState.Connecting -> WarningAmber
        ConnectionState.Connected -> Teal
        ConnectionState.Error -> DangerRed
    }
    val pulse by animateFloatAsState(
        targetValue = if (state == ConnectionState.Connected) 1f else 0.7f,
        animationSpec = tween(900),
        label = "pulse",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(220.dp)
            .clip(CircleShape)
            .background(BgCharcoal)
            .border(2.dp, ringColor, CircleShape),
    ) {
        Box(
            modifier = Modifier
                .size((196 * pulse).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(ringColor.copy(alpha = 0.18f), BgCharcoal),
                    ),
                ),
        )
        IconButton(
            onClick = onToggle,
            modifier = Modifier
                .size(132.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(TealDim, Teal),
                    ),
                ),
        ) {
            Icon(
                imageVector = Icons.Filled.PowerSettingsNew,
                contentDescription = null,
                tint = BgBlack,
                modifier = Modifier.size(48.dp),
            )
        }
    }
}

@Composable
private fun StatusLabel(state: ConnectionState) {
    val (text, color) = when (state) {
        ConnectionState.Disconnected -> "Отключено" to TextSecondary
        ConnectionState.Connecting -> "Соединение…" to WarningAmber
        ConnectionState.Connected -> "Подключено · защищено" to SuccessGreen
        ConnectionState.Error -> "Ошибка соединения" to DangerRed
    }
    AnimatedContent(targetState = text, label = "status") { label ->
        Text(
            label,
            color = color,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
        )
    }
}

@Composable
private fun ScanCard(
    scanState: ScanState,
    lastResult: ScanResult?,
    onScan: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceGrey),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Radar, contentDescription = null, tint = Teal)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Сканер блокировок РКН",
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.weight(1f))
                AnimatedVisibility(
                    visible = scanState is ScanState.Idle,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    androidx.compose.material3.AssistChip(
                        onClick = onScan,
                        label = { Text("Сканировать") },
                        colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
                            containerColor = TealDim,
                            labelColor = TextPrimary,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            when (scanState) {
                ScanState.Idle -> {
                    if (lastResult == null) {
                        Text(
                            "Нажмите «Сканировать», чтобы определить тип блокировки и подобрать оптимальный протокол.",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        ScanResultBlock(lastResult)
                    }
                }
                is ScanState.Running -> ScanProgress(scanState.step, scanState.totalSteps)
                is ScanState.Failed -> Text(
                    "Сканирование не удалось: ${scanState.message}",
                    color = DangerRed,
                )
            }
        }
    }
}

@Composable
private fun ScanProgress(step: Int, totalSteps: Int) {
    Column {
        Text(
            "Сканирование $step / $totalSteps…",
            color = Teal,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(8.dp))
        androidx.compose.material3.LinearProgressIndicator(
            progress = { step.toFloat() / totalSteps.coerceAtLeast(1) },
            modifier = Modifier.fillMaxWidth(),
            color = Teal,
            trackColor = SurfaceGreyElev,
        )
    }
}

@Composable
private fun ScanResultBlock(result: ScanResult) {
    Column {
        ResultRow("Метод блокировки", result.blockMethod.label)
        ResultRow("Рекомендуем", result.recommendedProtocol)
        ResultRow("Провайдер", result.isp ?: "—")
        ResultRow("Регион", result.geo ?: "—")
        ResultRow("Готовность", "${result.readinessScore}%")
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = TextSecondary, fontSize = 13.sp)
        Text(value, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 13.sp, textAlign = TextAlign.End)
    }
}

@Composable
private fun QuickStats(state: HomeUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatChip(
            icon = Icons.Outlined.Shield,
            title = "Подписка",
            value = state.subscriptionLabel,
            modifier = Modifier.weight(1f),
        )
        StatChip(
            icon = Icons.Outlined.Public,
            title = "Сервер",
            value = state.activeServer,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceGrey),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = Teal, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(title, color = TextSecondary, fontSize = 12.sp)
            }
            Spacer(Modifier.height(6.dp))
            Text(value, color = TextPrimary, fontWeight = FontWeight.SemiBold)
        }
    }
}
