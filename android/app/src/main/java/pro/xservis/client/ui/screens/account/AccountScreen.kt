package pro.xservis.client.ui.screens.account

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Help
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.PersonAddAlt1
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pro.xservis.client.ui.theme.BgBlack
import pro.xservis.client.ui.theme.SurfaceGrey
import pro.xservis.client.ui.theme.SurfaceGreyElev
import pro.xservis.client.ui.theme.Teal
import pro.xservis.client.ui.theme.TealDim
import pro.xservis.client.ui.theme.TextPrimary
import pro.xservis.client.ui.theme.TextSecondary

@Composable
fun AccountScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBlack)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ProfileHeader()
        SubscriptionCard()
        SectionList(
            "Реферальная программа",
            listOf(
                ListItem(Icons.Outlined.PersonAddAlt1, "Пригласить друга", "Получите 30 дней за каждого друга"),
                ListItem(Icons.Outlined.Receipt, "Партнёрский баланс", "0,00 ₽"),
            ),
        )
        SectionList(
            "Настройки",
            listOf(
                ListItem(Icons.Outlined.Devices, "Устройства", "1 из 3 активных"),
                ListItem(Icons.Outlined.Shield, "Kill switch & автоподключение"),
                ListItem(Icons.Outlined.Help, "Помощь и поддержка"),
                ListItem(Icons.Outlined.Info, "О приложении"),
                ListItem(Icons.Outlined.Logout, "Выйти", tint = pro.xservis.client.ui.theme.DangerRed),
            ),
        )
    }
}

@Composable
private fun ProfileHeader() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(TealDim, Teal))),
            contentAlignment = Alignment.Center,
        ) {
            Text("X", color = BgBlack, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text("Аккаунт xservis", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            Text("ID: # — войдите для синхронизации", color = TextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SubscriptionCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceGrey),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Подписка", color = TextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Text("Демо · 7 дней", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            Spacer(Modifier.height(2.dp))
            Text("Активна до: 12.05.2026", color = TextSecondary, fontSize = 12.sp)
        }
    }
}

private data class ListItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String? = null,
    val tint: androidx.compose.ui.graphics.Color = TextPrimary,
)

@Composable
private fun SectionList(title: String, items: List<ListItem>) {
    Column {
        Text(
            title.uppercase(),
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceGrey),
        ) {
            Column {
                items.forEachIndexed { idx, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(SurfaceGreyElev),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(item.icon, null, tint = item.tint, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, color = item.tint, fontWeight = FontWeight.Medium)
                            if (item.subtitle != null) {
                                Text(item.subtitle, color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                        Icon(Icons.Outlined.ChevronRight, null, tint = TextSecondary)
                    }
                    if (idx < items.lastIndex) {
                        androidx.compose.material3.HorizontalDivider(color = SurfaceGreyElev)
                    }
                }
            }
        }
    }
}
