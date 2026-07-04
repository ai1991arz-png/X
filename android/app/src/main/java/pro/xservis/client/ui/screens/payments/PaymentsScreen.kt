package pro.xservis.client.ui.screens.payments

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CurrencyBitcoin
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pro.xservis.client.ui.theme.BgBlack
import pro.xservis.client.ui.theme.BgCharcoal
import pro.xservis.client.ui.theme.SurfaceGrey
import pro.xservis.client.ui.theme.SurfaceGreyElev
import pro.xservis.client.ui.theme.Teal
import pro.xservis.client.ui.theme.TealBright
import pro.xservis.client.ui.theme.TealDim
import pro.xservis.client.ui.theme.TextPrimary
import pro.xservis.client.ui.theme.TextSecondary

private data class Plan(
    val id: String,
    val title: String,
    val months: Int,
    val price: Int,
    val priceOld: Int? = null,
    val popular: Boolean = false,
    val perks: List<String>,
)

private val PLANS = listOf(
    Plan("m1", "1 месяц", 1, 199, perks = listOf("3 устройства", "Все протоколы", "Авто-сканер блокировок")),
    Plan("m3", "3 месяца", 3, 499, priceOld = 597, popular = true, perks = listOf("3 устройства", "Все протоколы", "Скидка 17%")),
    Plan("m6", "6 месяцев", 6, 899, priceOld = 1194, perks = listOf("5 устройств", "Все протоколы", "Скидка 25%")),
    Plan("m12", "12 месяцев", 12, 1499, priceOld = 2388, perks = listOf("5 устройств", "Все протоколы", "Скидка 37%")),
)

@Composable
fun PaymentsScreen() {
    var selectedMethod by remember { mutableStateOf("sbp") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBlack)
            .padding(20.dp),
    ) {
        Text(
            "Тарифы",
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Оплата без верификации. СБП-рубли и криптовалюта — низкая комиссия.",
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MethodChip("СБП", "sbp", selectedMethod == "sbp", Icons.Outlined.AccountBalance) { selectedMethod = "sbp" }
            MethodChip("Крипта", "crypto", selectedMethod == "crypto", Icons.Outlined.CurrencyBitcoin) { selectedMethod = "crypto" }
        }
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(PLANS) { plan -> PlanCard(plan) }
        }
    }
}

@Composable
private fun MethodChip(
    label: String,
    @Suppress("UNUSED_PARAMETER") id: String,
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    androidx.compose.material3.AssistChip(
        onClick = onClick,
        label = { Text(label, color = if (selected) BgBlack else TextPrimary, fontWeight = FontWeight.Medium) },
        leadingIcon = { Icon(icon, null, tint = if (selected) BgBlack else Teal) },
        colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
            containerColor = if (selected) Teal else SurfaceGrey,
        ),
    )
}

@Composable
private fun PlanCard(plan: Plan) {
    val border = if (plan.popular) Teal else SurfaceGreyElev
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, border, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceGrey),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(plan.title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                if (plan.popular) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.linearGradient(listOf(TealDim, TealBright)))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Star, null, tint = BgBlack, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(2.dp))
                            Text("Популярный", color = BgBlack, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${plan.price} ₽", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 26.sp)
                Spacer(Modifier.width(8.dp))
                if (plan.priceOld != null) {
                    Text(
                        "${plan.priceOld} ₽",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            plan.perks.forEach { perk ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(Teal),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(perk, color = TextSecondary, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { /* invoke payment flow */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Teal, contentColor = BgBlack),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("Оплатить", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
