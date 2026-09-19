package com.example.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BubbleState
import com.example.model.MarketAnalysisResult
import com.example.model.SignalType
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun HomeScreen(
    isServiceRunning: Boolean,
    bubbleState: BubbleState,
    hasOverlayPermission: Boolean,
    latestResult: MarketAnalysisResult?,
    onRequestStartAnalyzer: () -> Unit,
    onStopAnalyzer: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onNavigateToSimulator: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0F291E)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CandlestickChart,
                    contentDescription = "Trading Analyzer",
                    tint = BullishGreen,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "AI TRADING ANALYZER",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Native Candlestick Screen Vision Engine",
                    color = BullishGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Text(
            text = "Analyze your visible trading chart in real time",
            color = TextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        // Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("status_card"),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "SYSTEM STATUS",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                StatusRow(
                    label = "Screen Capture Service",
                    isActive = isServiceRunning,
                    activeText = "ACTIVE",
                    inactiveText = if (isServiceRunning) "READY" else "OFF"
                )
                Spacer(modifier = Modifier.height(10.dp))
                StatusRow(
                    label = "Floating AI Bubble",
                    isActive = isServiceRunning && hasOverlayPermission,
                    activeText = "ON",
                    inactiveText = if (!hasOverlayPermission) "PERMISSION REQ" else "OFF"
                )
                Spacer(modifier = Modifier.height(10.dp))
                StatusRow(
                    label = "Screen Overlay Permission",
                    isActive = hasOverlayPermission,
                    activeText = "GRANTED",
                    inactiveText = "MISSING"
                )
            }
        }

        // Overlay Permission Warning Banner
        if (!hasOverlayPermission) {
            Spacer(modifier = Modifier.height(14.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onRequestOverlayPermission() },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF261805)),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Radio,
                        contentDescription = null,
                        tint = WarningAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Overlay Permission Required",
                            color = WarningAmber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Tap to grant 'Display over other apps' so the floating AI bubble can appear over trading charts.",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Main Action Button: START / STOP ANALYZER
        if (!isServiceRunning) {
            Button(
                onClick = onRequestStartAnalyzer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("start_analyzer_button"),
                colors = ButtonDefaults.buttonColors(containerColor = BullishGreen),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "START ANALYZER",
                    color = Color.Black,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    letterSpacing = 0.5.sp
                )
            }
        } else {
            Button(
                onClick = onStopAnalyzer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("stop_analyzer_button"),
                colors = ButtonDefaults.buttonColors(containerColor = BearishRed),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "STOP ANALYZER",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Latest Result Card (if any)
        latestResult?.let { res ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LATEST ANALYSIS",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Score: ${res.score}/100",
                            color = CyberCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    val (sigColor, sigText) = when (res.signal) {
                        SignalType.POSSIBLE_UP -> BullishGreen to "🟢 POSSIBLE UP"
                        SignalType.POSSIBLE_DOWN -> BearishRed to "🔴 POSSIBLE DOWN"
                        SignalType.WAIT -> WarningAmber to "🟡 WAIT"
                    }

                    Text(
                        text = sigText,
                        color = sigColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Pattern: ${res.pattern} • Trend: ${res.trend}",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Secondary Navigation Cards
        Text(
            text = "QUICK TOOLS & SETTINGS",
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )

        NavigationActionItem(
            title = "Live Chart Sandbox / Simulator",
            subtitle = "Test chart detection & multi-factor engine with simulated candles",
            icon = Icons.Default.CandlestickChart,
            tint = CyberCyan,
            onClick = onNavigateToSimulator
        )
        Spacer(modifier = Modifier.height(10.dp))

        NavigationActionItem(
            title = "Analysis History",
            subtitle = "Review past signals, scores, and technical reasons",
            icon = Icons.Default.History,
            tint = BullishGreen,
            onClick = onNavigateToHistory
        )
        Spacer(modifier = Modifier.height(10.dp))

        NavigationActionItem(
            title = "Settings",
            subtitle = "Floating bubble, auto-analysis interval, sound & haptics",
            icon = Icons.Default.Settings,
            tint = WarningAmber,
            onClick = onNavigateToSettings
        )
        Spacer(modifier = Modifier.height(10.dp))

        NavigationActionItem(
            title = "About & Disclaimer",
            subtitle = "Algorithmic scope, risk warnings & multi-factor rules",
            icon = Icons.Default.Info,
            tint = TextSecondary,
            onClick = onNavigateToAbout
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Mandatory PRD Disclaimer at footer
        Text(
            text = "Algorithmic analysis only. No signal is guaranteed. Trading involves substantial financial risk. The application does not execute trades automatically.",
            color = TextMuted,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun StatusRow(
    label: String,
    isActive: Boolean,
    activeText: String,
    inactiveText: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = TextPrimary,
            fontSize = 13.sp
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isActive) BullishGreen else TextMuted)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isActive) activeText else inactiveText,
                color = if (isActive) BullishGreen else TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun NavigationActionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}
