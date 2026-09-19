package com.example.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AutoTradeConfig
import com.example.model.AutoTradeOrder
import com.example.model.OrderStatus
import com.example.model.SignalType
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoTradeScreen(
    config: AutoTradeConfig,
    orders: List<AutoTradeOrder>,
    onConfigChanged: (AutoTradeConfig) -> Unit,
    onDeleteOrder: (Long) -> Unit,
    onClearAllOrders: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val totalTrades = orders.size
    val winningTrades = orders.count { it.status == OrderStatus.CLOSED_WIN }
    val winRate = if (totalTrades > 0) (winningTrades * 100f / totalTrades).toInt() else 0
    val netPnl = orders.sumOf { it.pnl }
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss • dd MMM", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoMode,
                            contentDescription = null,
                            tint = if (config.enabled) BullishGreen else TextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Auto Trade Bot",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    if (orders.isNotEmpty()) {
                        IconButton(onClick = onClearAllOrders) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear History",
                                tint = TextMuted
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Switch Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("auto_trade_toggle_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (config.enabled) Color(0xFF0D251A) else DarkSurface
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (config.enabled) BullishGreen else DarkBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (config.enabled) BullishGreen else TextMuted)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (config.enabled) "AUTO TRADE ACTIVE" else "AUTO TRADE PAUSED",
                                    color = if (config.enabled) BullishGreen else TextMuted,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (config.enabled)
                                    "Triggering instant trade execution on UP/DOWN signals (Score ≥ ${config.minConfidenceScore})"
                                else
                                    "Enable to auto-execute UP/DOWN trades when screen analysis completes",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }

                        Switch(
                            checked = config.enabled,
                            onCheckedChange = { isEnabled ->
                                onConfigChanged(config.copy(enabled = isEnabled))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = BullishGreen,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = DarkSurfaceVariant
                            )
                        )
                    }
                }
            }

            // Bot Performance Statistics
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "BOT TRADING STATS",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Net PnL", color = TextSecondary, fontSize = 11.sp)
                                Text(
                                    text = "${if (netPnl >= 0) "+$" else "-$"}${String.format(Locale.US, "%.2f", abs(netPnl))}",
                                    color = if (netPnl >= 0) BullishGreen else BearishRed,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Win Rate", color = TextSecondary, fontSize = 11.sp)
                                Text(
                                    text = "$winRate%",
                                    color = CyberCyan,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Total Filled", color = TextSecondary, fontSize = 11.sp)
                                Text(
                                    text = "$totalTrades trades",
                                    color = TextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }

            // Auto Trade Parameters Configuration
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "AUTO EXECUTION SETTINGS",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // Trade Size selector
                        Text("Trade Size ($ USD)", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(5.0, 10.0, 25.0, 50.0, 100.0).forEach { amt ->
                                FilterChip(
                                    selected = config.tradeAmount == amt,
                                    onClick = { onConfigChanged(config.copy(tradeAmount = amt)) },
                                    label = { Text("$$amt") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BullishGreen,
                                        selectedLabelColor = Color.Black
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Minimum Confidence Filter
                        Text("Min Confidence Score Threshold", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Only executes when engine multi-factor score matches or exceeds threshold", color = TextMuted, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(70, 75, 80, 85, 90).forEach { score ->
                                FilterChip(
                                    selected = config.minConfidenceScore == score,
                                    onClick = { onConfigChanged(config.copy(minConfidenceScore = score)) },
                                    label = { Text("$score%+") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyberCyan,
                                        selectedLabelColor = Color.Black
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Order History Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EXECUTED ORDERS (${orders.size})",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Executed Orders List
            if (orders.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoMode,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No Auto Trades Executed Yet",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "When Auto Trade is active, every screen scan will instantly place UP/DOWN orders automatically.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(orders, key = { it.id }) { order ->
                    OrderCardItem(
                        order = order,
                        formattedTime = timeFormat.format(Date(order.timestamp)),
                        onDelete = { onDeleteOrder(order.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Algorithmic simulation and execution engine. Market risks apply. Always monitor open orders.",
                    color = TextMuted,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun OrderCardItem(
    order: AutoTradeOrder,
    formattedTime: String,
    onDelete: () -> Unit
) {
    val isWin = order.status == OrderStatus.CLOSED_WIN
    val isUp = order.direction == SignalType.UP

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (isWin) Color(0x3300E676) else Color(0x33FF5252))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isUp) BullishGreen else BearishRed)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (isUp) "▲ UP" else "▼ DOWN",
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = order.asset,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Entry: ${order.entryPrice} • Size: $${order.amount}",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Text(
                    text = "${if (order.pnl >= 0) "+$" else "-$"}${String.format(Locale.US, "%.2f", abs(order.pnl))}",
                    color = if (isWin) BullishGreen else BearishRed,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Score: ${order.score}% • ${order.triggerReason.take(32)}...",
                    color = TextMuted,
                    fontSize = 11.sp
                )
                Text(
                    text = formattedTime,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}
