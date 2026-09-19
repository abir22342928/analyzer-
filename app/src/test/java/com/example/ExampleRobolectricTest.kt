package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.analysis.TechnicalAnalysisEngine
import com.example.model.ChartDetectionResult
import com.example.model.SignalType
import com.example.vision.SampleCharts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Trading Analyzer", appName)
  }

  @Test
  fun `test bullish breakout analysis scenario`() {
    val candles = SampleCharts.getCandlesForScenario(SampleCharts.Scenario.BULLISH_BREAKOUT)
    val detection = ChartDetectionResult(
        detected = true,
        candles = candles,
        asset = "EUR/USD",
        timeframe = "M1"
    )
    val result = TechnicalAnalysisEngine.analyzeChart(detection)
    assertNotNull(result)
    assertEquals(SignalType.POSSIBLE_UP, result.signal)
    assertTrue(result.score >= 65)
    assertTrue(result.reasons.isNotEmpty())
  }

  @Test
  fun `test bearish rejection analysis scenario`() {
    val candles = SampleCharts.getCandlesForScenario(SampleCharts.Scenario.BEARISH_REJECTION)
    val detection = ChartDetectionResult(
        detected = true,
        candles = candles,
        asset = "BTC/USDT",
        timeframe = "M5"
    )
    val result = TechnicalAnalysisEngine.analyzeChart(detection)
    assertNotNull(result)
    assertEquals(SignalType.POSSIBLE_DOWN, result.signal)
    assertTrue(result.score >= 65)
  }

  @Test
  fun `test ranging consolidation defaults to wait`() {
    val candles = SampleCharts.getCandlesForScenario(SampleCharts.Scenario.RANGING_CONSOLIDATION)
    val detection = ChartDetectionResult(
        detected = true,
        candles = candles,
        asset = "RANGE",
        timeframe = "M1"
    )
    val result = TechnicalAnalysisEngine.analyzeChart(detection)
    assertNotNull(result)
    assertEquals(SignalType.WAIT, result.signal)
  }
}

