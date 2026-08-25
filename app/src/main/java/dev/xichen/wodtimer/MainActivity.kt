package dev.xichen.wodtimer

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.xichen.wodtimer.timer.TimerStatus
import dev.xichen.wodtimer.ui.AppScreen
import dev.xichen.wodtimer.ui.AppViewModel
import dev.xichen.wodtimer.ui.WodTimerApp

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val timer by viewModel.timer.collectAsStateWithLifecycle()
            val screen by viewModel.screen.collectAsStateWithLifecycle()
            val keepScreenAwake = shouldKeepScreenAwake(screen, timer?.status)
            DisposableEffect(keepScreenAwake) {
                if (keepScreenAwake) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                onDispose { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
            }
            val notificationPermission = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { viewModel.startTimer() }
            BackHandler(enabled = screen != AppScreen.HOME) { viewModel.back() }
            WodTimerApp(
                viewModel = viewModel,
                onStart = {
                    if (Build.VERSION.SDK_INT >= 33 &&
                        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
                    ) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else viewModel.startTimer()
                },
            )
        }
    }
}

internal fun shouldKeepScreenAwake(screen: AppScreen, status: TimerStatus?): Boolean =
    screen == AppScreen.TIMER && status in setOf(
        TimerStatus.PREPARING,
        TimerStatus.RUNNING,
        TimerStatus.PAUSED,
    )
