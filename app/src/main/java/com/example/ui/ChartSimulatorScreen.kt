package com.example.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.analysis.TechnicalAnalysisEngine
import com.example.model.CandleModel
import com.example.model.ChartDetectionResult
import com.example.model.MarketAnalysisResult
import com.example.model.SignalType
import com.example.trade.AutoTradeEngine
import com.example.voice.AiVoiceSpeaker
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
import com.example.vision.SampleCharts
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartSimulatorScreen(
    onNavigateBack: () -> Unit,
    onSaveToHistory: (MarketAnalysisResult) -> Unit
) {
    var selectedScenario by remember { mutableStateOf(SampleCharts.Scenario.BULLISH_BREAKOUT) }
    var currentCandles by remember(selectedScenario) {
        mutableStateOf(SampleCharts.getCandlesForScenario(selectedScenario))
    }

    var isScanning by remember { mutableStateOf(false) }
    var scanProgressText by remember { mutableStateOf("") }
    var analysisResult by remember { mutableStateOf<MarketAnalysisResult?>(null) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Chart Sandbox & Simulator",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Simulate live charts directly on screen to test vision & technical analysis without needing an external broker.",
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Scenario Selector Chips
            Text(
                text = "SELECT MARKET SCENARIO",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            SampleCharts.Scenario.values().forEach { scenario ->
                val isSelected = scenario == selectedScenario
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            selectedScenario = scenario
                            analysisResult = null
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFF132238) else DarkSurface
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) CyberCyan else DarkBorder
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = scenario.title,
                            color = if (isSelected) CyberCyan else TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = scenario.description,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Realistic Candlestick Canvas
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0C101B)),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CandlestickChartCanvas(
                        candles = currentCandles,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Overlay scanning animation if in progress
                    if (isScanning) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x99000000)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "SCANNING CHART...",
                                    color = BullishGreen,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = scanProgressText,
                                    color = TextPrimary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Run Analysis Button
            Button(
                onClick = {
                    scope.launch {
                        isScanning = true
                        scanProgressText = "Reading visible candlesticks..."
                        delay(400)
                        scanProgressText = "Analyzing trend & support/resistance..."
                        delay(400)
                        scanProgressText = "Calculating multi-factor confirmation..."
                        delay(350)

                        val detection = ChartDetectionResult(
                            detected = true,
                            candles = currentCandles,
                            asset = "SIM / ${selectedScenario.name}",
                            timeframe = "M1",
                            currentPrice = String.format("%.2f", currentCandles.last().close)
                        )
                        val result = TechnicalAnalysisEngine.analyzeChart(detection)
                        analysisResult = result
                        onSaveToHistory(result)
                        AutoTradeEngine.evaluateAndExecute(result)
                        isScanning = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("run_analysis_button"),
                colors = ButtonDefaults.buttonColors(containerColor = BullishGreen),
                shape = RoundedCornerShape(12.dp),
                enabled = !isScanning
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isScanning) "ANALYZING..." else "TEST ANALYZER ON THIS CHART",
                    color = Color.Black,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
            }

            // Analysis Result Presentation Card
            analysisResult?.let { result ->
                Spacer(modifier = Modifier.height(20.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "NEXT CANDLE PREDICTION",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val (sigColor, sigText) = when (result.signal) {
                            SignalType.UP -> BullishGreen to "🟢 UP"
                            SignalType.DOWN -> BearishRed to "🔴 DOWN"
                        }

                        Text(
                            text = sigText,
                            color = sigColor,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Analysis Score: ${result.score}/100",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                        )

                        // Auto Trade Execution notification badge
                        if (AutoTradeEngine.config.value.enabled) {
                            val lastTrade = AutoTradeEngine.lastExecutedTrade.value
                            if (lastTrade != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (lastTrade.pnl >= 0) Color(0x2200E676) else Color(0x22FF5252))
                                        .border(1.dp, if (lastTrade.pnl >= 0) BullishGreen else BearishRed, RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "⚡ Auto Trade: ${lastTrade.direction.name} @ ${lastTrade.entryPrice}",
                                            color = TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${if (lastTrade.pnl >= 0) "+$" else "-$"}${String.format(java.util.Locale.US, "%.2f", kotlin.math.abs(lastTrade.pnl))}",
                                            color = if (lastTrade.pnl >= 0) BullishGreen else BearishRed,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                            }
                        }

                        Text(text = "Trend: ${result.trend}", color = TextSecondary, fontSize = 12.sp)
                        Text(text = "Momentum: ${result.momentum}", color = TextSecondary, fontSize = 12.sp)
                        Text(text = "Structure: ${result.structure}", color = TextSecondary, fontSize = 12.sp)
                        Text(text = "Pattern: ${result.pattern}", color = TextSecondary, fontSize = 12.sp)
                        Text(text = "S/R Level: ${result.supportResistance}", color = TextSecondary, fontSize = 12.sp)

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Analysis Reasons:",
                            color = CyberCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        result.reasons.forEach { r ->
                            Text(text = "✓ $r", color = TextPrimary, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                        }

                        if (result.riskFactors.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Risk Factors:",
                                color = WarningAmber,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            result.riskFactors.forEach { rf ->
                                Text(text = "! $rf", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = result.disclaimer,
                            color = TextMuted,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CandlestickChartCanvas(
    candles: List<CandleModel>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.padding(16.dp)) {
        if (candles.isEmpty()) return@Canvas

        var minPrice = Float.MAX_VALUE
        var maxPrice = Float.MIN_VALUE
        for (c in candles) {
            if (c.low < minPrice) minPrice = c.low
            if (c.high > maxPrice) maxPrice = c.high
        }
        val padding = (maxPrice - minPrice) * 0.1f
        minPrice -= padding
        maxPrice += padding
        val priceRange = (maxPrice - minPrice).coerceAtLeast(0.01f)

        // Draw horizontal grid lines
        val gridLines = 4
        for (i in 0..gridLines) {
            val y = size.height * (i.toFloat() / gridLines)
            drawLine(
                color = Color(0xFF1E293B),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
        }

        // Draw candles
        val candleCount = candles.size
        val slotWidth = size.width / candleCount
        val candleBodyWidth = (slotWidth * 0.6f).coerceIn(6f, 32f)

        for (i in candles.indices) {
            val c = candles[i]
            val xCenter = slotWidth * i + slotWidth / 2f

            fun priceToY(price: Float): Float {
                return size.height - ((price - minPrice) / priceRange * size.height)
            }

            val highY = priceToY(c.high)
            val lowY = priceToY(c.low)
            val openY = priceToY(c.open)
            val closeY = priceToY(c.close)

            val color = if (c.isBullish) Color(0xFF00E676) else Color(0xFFFF5252)

            // Draw wick
            drawLine(
                color = color,
                start = Offset(xCenter, highY),
                end = Offset(xCenter, lowY),
                strokeWidth = 2f
            )

            // Draw body
            val topBodyY = min(openY, closeY)
            val bodyHeight = max(abs(openY - closeY), 3f)
            drawRect(
                color = color,
                topLeft = Offset(xCenter - candleBodyWidth / 2f, topBodyY),
                size = Size(candleBodyWidth, bodyHeight)
            )
        }
    }
}
