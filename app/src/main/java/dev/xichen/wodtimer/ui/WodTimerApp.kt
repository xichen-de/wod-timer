package dev.xichen.wodtimer.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun WodTimerApp(viewModel: AppViewModel, onStart: () -> Unit) {
    val screen by viewModel.screen.collectAsStateWithLifecycle()
    WodTimerTheme {
        Surface(Modifier.fillMaxSize()) {
            when (screen) {
                AppScreen.HOME -> HomeScreen(viewModel)
                AppScreen.CONFIGURE -> ConfigureScreen(viewModel)
                AppScreen.TIMER -> ActiveTimerScreen(viewModel, onStart)
            }
        }
    }
}
