package pro.xservis.client.ui.screens.servers

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pro.xservis.client.ui.theme.BgBlack
import pro.xservis.client.ui.theme.SurfaceGrey
import pro.xservis.client.ui.theme.SurfaceGreyElev
import pro.xservis.client.ui.theme.SuccessGreen
import pro.xservis.client.ui.theme.Teal
import pro.xservis.client.ui.theme.TextPrimary
import pro.xservis.client.ui.theme.TextSecondary
import pro.xservis.client.ui.theme.WarningAmber

private data class ServerInfo(
    val name: String,
    val country: String,
    val flag: String,
    val pingMs: Int,
    val loadPct: Int,
    val protocols: List<String>,
)

private val SAMPLE_SERVERS = listOf(
    ServerInfo("Москва · Reality #1", "Россия", "\uD83C\uDDF7\uD83C\uDDFA", 14, 38, listOf("Reality", "AmneziaWG")),
    ServerInfo("Санкт-Петербург", "Россия", "\uD83C\uDDF7\uD83C\uDDFA", 22, 51, listOf("Reality", "VLESS-WS")),
    ServerInfo("Новосибирск", "Россия", "\uD83C\uDDF7\uD83C\uDDFA", 41, 24, listOf("Reality")),
    ServerInfo("Frankfurt · DE-1", "Германия", "\uD83C\uDDE9\uD83C\uDDEA", 58, 64, listOf("Reality", "Trojan", "AmneziaWG")),
    ServerInfo("Amsterdam · NL-1", "Нидерланды", "\uD83C\uDDF3\uD83C\uDDF1", 52, 47, listOf("Reality", "Shadowsocks-2022")),
    ServerInfo("New York · US-East", "США", "\uD83C\uDDFA\uD83C\uDDF8", 121, 33, listOf("Reality", "AmneziaWG")),
    ServerInfo("Tokyo · JP-1", "Япония", "\uD83C\uDDEF\uD83C\uDDF5", 188, 40, listOf("Reality")),
    ServerInfo("Singapore · SG-1", "Сингапур", "\uD83C\uDDF8\uD83C\uDDEC", 168, 44, listOf("Reality", "Trojan")),
)

@Composable
fun ServersScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBlack)
            .padding(20.dp),
    ) {
        Text(
            "Серверы",
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Мега-быстрая сеть, оптимизированная под YouTube, видеозвонки, Discord, Telegram-звонки.",
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(SAMPLE_SERVERS) { server ->
                ServerRow(server)
            }
        }
    }
}

@Composable
private fun ServerRow(s: ServerInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceGrey),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SurfaceGreyElev),
                contentAlignment = Alignment.Center,
            ) {
                Text(s.flag, fontSize = 22.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(s.name, color = TextPrimary, fontWeight = FontWeight.Medium)
                Text(s.country, color = TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    s.protocols.forEach { p ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceGreyElev)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(p, color = Teal, fontSize = 10.sp)
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Bolt, null, tint = pingColor(s.pingMs), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("${s.pingMs} мс", color = pingColor(s.pingMs), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.SignalCellularAlt, null, tint = loadColor(s.loadPct), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("${s.loadPct}%", color = loadColor(s.loadPct), fontSize = 12.sp)
                }
            }
        }
    }
}

private fun pingColor(ping: Int): Color = when {
    ping < 50 -> SuccessGreen
    ping < 120 -> Teal
    else -> WarningAmber
}

private fun loadColor(load: Int): Color = when {
    load < 50 -> SuccessGreen
    load < 75 -> Teal
    else -> WarningAmber
}
