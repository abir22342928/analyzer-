package com.example

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.database.AppDatabase
import com.example.database.HistoryRepository
import com.example.model.AppSettings
import com.example.model.MarketAnalysisResult
import com.example.service.TradingAnalyzerService
import com.example.ui.AboutScreen
import com.example.ui.ChartSimulatorScreen
import com.example.ui.HistoryScreen
import com.example.ui.HomeScreen
import com.example.ui.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AppScreen {
    HOME,
    SIMULATOR,
    HISTORY,
    SETTINGS,
    ABOUT
}

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_OPEN_SCREEN = "EXTRA_OPEN_SCREEN"
        const val SCREEN_SIMULATOR = "SIMULATOR"
    }

    private lateinit var prefs: SharedPreferences
    private lateinit var historyRepository: HistoryRepository
    private var screenNavigationCallback: ((AppScreen) -> Unit)? = null

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        checkIntentForScreen(intent)
    }

    private fun checkIntentForScreen(intent: Intent?) {
        if (intent?.getStringExtra(EXTRA_OPEN_SCREEN) == SCREEN_SIMULATOR) {
            screenNavigationCallback?.invoke(AppScreen.SIMULATOR)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        prefs = getSharedPreferences("trading_analyzer_prefs", Context.MODE_PRIVATE)
        val db = AppDatabase.getDatabase(this)
        historyRepository = HistoryRepository(db.analysisHistoryDao())

        setContent {
            val initial = if (intent?.getStringExtra(EXTRA_OPEN_SCREEN) == SCREEN_SIMULATOR) {
                AppScreen.SIMULATOR
            } else {
                AppScreen.HOME
            }
            var currentScreen by remember { mutableStateOf(initial) }
            val scope = rememberCoroutineScope()

            DisposableEffect(Unit) {
                screenNavigationCallback = { targetScreen ->
                    currentScreen = targetScreen
                }
                onDispose {
                    screenNavigationCallback = null
                }
            }

            var settings by remember {
                mutableStateOf(
                    AppSettings(
                        floatingBubble = prefs.getBoolean("floating_bubble", true),
                        autoAnalysis = prefs.getBoolean("auto_analysis", false),
                        analysisIntervalSeconds = prefs.getInt("analysis_interval", 30),
                        sound = prefs.getBoolean("sound", true),
                        vibration = prefs.getBoolean("vibration", true),
                        showScore = prefs.getBoolean("show_score", true),
                        showReasons = prefs.getBoolean("show_reasons", true),
                        darkTheme = prefs.getBoolean("dark_theme", true)
                    )
                )
            }

            fun updateSettings(newSettings: AppSettings) {
                settings = newSettings
                prefs.edit()
                    .putBoolean("floating_bubble", newSettings.floatingBubble)
                    .putBoolean("auto_analysis", newSettings.autoAnalysis)
                    .putInt("analysis_interval", newSettings.analysisIntervalSeconds)
                    .putBoolean("sound", newSettings.sound)
                    .putBoolean("vibration", newSettings.vibration)
                    .putBoolean("show_score", newSettings.showScore)
                    .putBoolean("show_reasons", newSettings.showReasons)
                    .putBoolean("dark_theme", newSettings.darkTheme)
                    .apply()

                TradingAnalyzerService.autoAnalysisIntervalSec =
                    if (newSettings.autoAnalysis) newSettings.analysisIntervalSeconds else 0
            }

            var hasOverlayPermission by remember {
                mutableStateOf(Settings.canDrawOverlays(this@MainActivity))
            }

            val isServiceRunning by TradingAnalyzerService.isRunning.collectAsStateWithLifecycle()
            val latestResult by TradingAnalyzerService.latestResult.collectAsStateWithLifecycle()
            val bubbleState by TradingAnalyzerService.bubbleState.collectAsStateWithLifecycle()
            val historyList by historyRepository.allHistory.collectAsStateWithLifecycle(initialValue = emptyList())

            // Screen Capture Intent Launcher
            val screenCaptureLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                    val serviceIntent = Intent(this@MainActivity, TradingAnalyzerService::class.java).apply {
                        action = TradingAnalyzerService.ACTION_START
                        putExtra(TradingAnalyzerService.EXTRA_RESULT_CODE, result.resultCode)
                        putExtra(TradingAnalyzerService.EXTRA_DATA_INTENT, result.data)
                    }
                    ContextCompat.startForegroundService(this@MainActivity, serviceIntent)
                    Toast.makeText(this@MainActivity, "AI Analyzer Started. Floating bubble active.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Screen capture permission was not granted", Toast.LENGTH_SHORT).show()
                }
            }

            // Notification Permission Launcher (Android 13+)
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                // proceed
            }

            // Overlay Permission Launcher
            val overlayPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) {
                hasOverlayPermission = Settings.canDrawOverlays(this@MainActivity)
            }

            fun requestStartAnalyzer() {
                // 1. Check Notification permission on Android 13+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                // 2. Check Overlay permission
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    overlayPermissionLauncher.launch(intent)
                    Toast.makeText(this@MainActivity, "Please grant 'Display over other apps' to enable the floating AI bubble", Toast.LENGTH_LONG).show()
                    return
                }

                // 3. Request MediaProjection
                val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                screenCaptureLauncher.launch(projectionManager.createScreenCaptureIntent())
            }

            fun stopAnalyzer() {
                val serviceIntent = Intent(this@MainActivity, TradingAnalyzerService::class.java).apply {
                    action = TradingAnalyzerService.ACTION_STOP
                }
                startService(serviceIntent)
                Toast.makeText(this@MainActivity, "Trading Analyzer stopped", Toast.LENGTH_SHORT).show()
            }

            BackHandler(enabled = currentScreen != AppScreen.HOME) {
                currentScreen = AppScreen.HOME
            }

            MyApplicationTheme(darkTheme = settings.darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    when (currentScreen) {
                        AppScreen.HOME -> HomeScreen(
                            isServiceRunning = isServiceRunning,
                            bubbleState = bubbleState,
                            hasOverlayPermission = hasOverlayPermission,
                            latestResult = latestResult,
                            onRequestStartAnalyzer = { requestStartAnalyzer() },
                            onStopAnalyzer = { stopAnalyzer() },
                            onRequestOverlayPermission = {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:$packageName")
                                )
                                overlayPermissionLauncher.launch(intent)
                            },
                            onNavigateToSimulator = { currentScreen = AppScreen.SIMULATOR },
                            onNavigateToHistory = { currentScreen = AppScreen.HISTORY },
                            onNavigateToSettings = { currentScreen = AppScreen.SETTINGS },
                            onNavigateToAbout = { currentScreen = AppScreen.ABOUT }
                        )

                        AppScreen.SIMULATOR -> ChartSimulatorScreen(
                            onNavigateBack = { currentScreen = AppScreen.HOME },
                            onSaveToHistory = { res ->
                                scope.launch(Dispatchers.IO) {
                                    historyRepository.saveResult(res)
                                }
                            }
                        )

                        AppScreen.HISTORY -> HistoryScreen(
                            historyList = historyList,
                            onNavigateBack = { currentScreen = AppScreen.HOME },
                            onDeleteItem = { id ->
                                scope.launch(Dispatchers.IO) {
                                    historyRepository.deleteById(id)
                                }
                            },
                            onClearAll = {
                                scope.launch(Dispatchers.IO) {
                                    historyRepository.clearAll()
                                }
                            }
                        )

                        AppScreen.SETTINGS -> SettingsScreen(
                            settings = settings,
                            onSettingsChanged = { updateSettings(it) },
                            onNavigateBack = { currentScreen = AppScreen.HOME }
                        )

                        AppScreen.ABOUT -> AboutScreen(
                            onNavigateBack = { currentScreen = AppScreen.HOME }
                        )
                    }
                }
            }
        }
    }
}
