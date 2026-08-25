package dev.xichen.wodtimer.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.xichen.wodtimer.timer.TimerMode
import dev.xichen.wodtimer.timer.TimerPhase
import dev.xichen.wodtimer.timer.TimerSnapshot
import dev.xichen.wodtimer.timer.TimerStatus
import dev.xichen.wodtimer.timer.formatClock
import dev.xichen.wodtimer.timer.formatElapsed
import dev.xichen.wodtimer.timer.title

@Composable
fun ActiveTimerScreen(viewModel: AppViewModel, onStart: () -> Unit) {
    val state by viewModel.timer.collectAsStateWithLifecycle()
    val timer = state ?: return
    var confirmReset by remember { mutableStateOf(false) }
    val phaseColor = timer.phaseColor()
    val canLeave = timer.status == TimerStatus.IDLE || timer.status == TimerStatus.FINISHED

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(phaseColor.copy(alpha = .09f), MaterialTheme.colorScheme.background)))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TimerHeader(timer = timer, canLeave = canLeave, onBack = viewModel::back)
        Spacer(Modifier.height(14.dp))
        TimerConsole(timer = timer, phaseColor = phaseColor, modifier = Modifier.weight(1f))
        Spacer(Modifier.height(16.dp))
        TimerControls(
            status = timer.status,
            onStart = onStart,
            onPause = viewModel::pauseTimer,
            onResume = viewModel::resumeTimer,
            onStop = viewModel::stopTimer,
            onDone = viewModel::back,
            onReset = {
                if (timer.status == TimerStatus.IDLE) viewModel.resetTimer() else confirmReset = true
            },
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            icon = { Icon(Icons.Default.Close, null) },
            title = { Text("Reset this workout?") },
            text = { Text("The current timer progress will be cleared.") },
            confirmButton = {
                Button(onClick = { confirmReset = false; viewModel.resetTimer() }) { Text("Reset") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Keep going") } },
        )
    }
}

@Composable
private fun TimerHeader(timer: TimerSnapshot, canLeave: Boolean, onBack: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(52.dp), contentAlignment = Alignment.Center) {
        if (canLeave) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                timer.config.mode.title().uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(timer.status.label(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TimerConsole(timer: TimerSnapshot, phaseColor: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        border = BorderStroke(1.dp, phaseColor.copy(alpha = .28f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = .94f)),
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                shape = CircleShape,
                color = phaseColor.copy(alpha = .14f),
                border = BorderStroke(1.dp, phaseColor.copy(alpha = .24f)),
            ) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).background(phaseColor, CircleShape))
                    Text(
                        timer.phaseLabel(),
                        Modifier.padding(start = 8.dp),
                        color = phaseColor,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                    )
                }
            }
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = timer.displayTime(),
                    color = phaseColor,
                    fontSize = if (timer.phase == TimerPhase.PREPARING) 132.sp else 82.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-3).sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
                Text(
                    timer.timeCaption(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge,
                    letterSpacing = 1.1.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            timer.progress()?.let { progress ->
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = phaseColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Spacer(Modifier.height(18.dp))
            }
            TimerStats(timer)
        }
    }
}

@Composable
private fun TimerStats(timer: TimerSnapshot) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (timer.currentRound != null) {
            StatTile("ROUND", "${timer.currentRound} / ${timer.totalRounds}", Modifier.weight(1f))
        }
        StatTile("TOTAL TIME", formatElapsed(timer.elapsedMillis), Modifier.weight(1f))
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .7f)) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = .8.sp)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun TimerControls(
    status: TimerStatus,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onDone: () -> Unit,
    onReset: () -> Unit,
) {
    val primaryLabel = when (status) {
        TimerStatus.IDLE -> "Start workout"
        TimerStatus.PREPARING, TimerStatus.RUNNING -> "Pause"
        TimerStatus.PAUSED -> "Resume"
        TimerStatus.FINISHED -> "Done"
    }
    val primaryIcon = if (status == TimerStatus.PREPARING || status == TimerStatus.RUNNING) Icons.Default.Pause else Icons.Default.PlayArrow
    val primaryAction = when (status) {
        TimerStatus.IDLE -> onStart
        TimerStatus.PREPARING, TimerStatus.RUNNING -> onPause
        TimerStatus.PAUSED -> onResume
        TimerStatus.FINISHED -> onDone
    }
    Button(
        onClick = primaryAction,
        modifier = Modifier.fillMaxWidth().height(64.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(20.dp),
    ) {
        Icon(primaryIcon, null, Modifier.size(23.dp))
        Text(primaryLabel, Modifier.padding(start = 9.dp), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
    }
    if (status != TimerStatus.FINISHED) {
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (status != TimerStatus.IDLE) {
                OutlinedButton(onClick = onStop, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(16.dp)) {
                    Text("Stop", fontWeight = FontWeight.Bold)
                }
            }
            TextButton(onClick = onReset, modifier = Modifier.weight(1f).height(50.dp)) {
                Text("Reset", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TimerSnapshot.phaseColor(): Color = when (phase) {
    TimerPhase.WORK -> if (isSystemInDarkTheme()) Color(0xFF55E393) else Color(0xFF147748)
    TimerPhase.REST -> if (isSystemInDarkTheme()) Color(0xFF69C7FF) else Color(0xFF12668B)
    TimerPhase.FINISHED -> MaterialTheme.colorScheme.primary
    TimerPhase.PREPARING -> if (isSystemInDarkTheme()) Color(0xFFFFA94D) else Color(0xFF945000)
    else -> MaterialTheme.colorScheme.onBackground
}

private fun TimerSnapshot.displayTime(): String = when {
    phase == TimerPhase.PREPARING -> preStartRemainingSeconds?.toString() ?: "3"
    config.mode is TimerMode.ForTime -> formatElapsed(elapsedMillis)
    intervalRemainingMillis != null -> formatClock(intervalRemainingMillis)
    else -> formatClock(remainingMillis ?: 0L)
}

private fun TimerSnapshot.phaseLabel(): String = when (phase) {
    TimerPhase.PREPARING -> "GET READY"
    TimerPhase.WORK -> "WORK"
    TimerPhase.REST -> "REST"
    TimerPhase.FINISHED -> "COMPLETE"
    TimerPhase.RUNNING -> when (config.mode) {
        TimerMode.ForTime -> "FOR TIME"
        is TimerMode.Amrap -> "AMRAP"
        is TimerMode.EveryXMinutes -> "ROUND IN PROGRESS"
        is TimerMode.Intervals -> "WORK"
    }
}

private fun TimerSnapshot.timeCaption(): String = when {
    phase == TimerPhase.PREPARING -> "STARTING IN"
    config.mode is TimerMode.ForTime -> "ELAPSED"
    config.mode is TimerMode.EveryXMinutes -> "UNTIL NEXT ROUND"
    phase == TimerPhase.WORK -> "WORK REMAINING"
    phase == TimerPhase.REST -> "REST REMAINING"
    else -> "REMAINING"
}

private fun TimerSnapshot.progress(): Float? {
    if (phase == TimerPhase.PREPARING) {
        val total = config.preStartSeconds.coerceAtLeast(1).toFloat()
        return (1f - (preStartRemainingSeconds ?: 0) / total).coerceIn(0f, 1f)
    }
    val remaining: Long
    val duration: Long
    when (val mode = config.mode) {
        TimerMode.ForTime -> return null
        is TimerMode.Amrap -> { remaining = remainingMillis ?: 0L; duration = mode.durationMillis }
        is TimerMode.EveryXMinutes -> { remaining = intervalRemainingMillis ?: 0L; duration = mode.intervalMillis }
        is TimerMode.Intervals -> {
            remaining = intervalRemainingMillis ?: 0L
            duration = if (phase == TimerPhase.REST) mode.restMillis else mode.workMillis
        }
    }
    if (duration <= 0) return null
    return (1f - remaining.toFloat() / duration).coerceIn(0f, 1f)
}

private fun TimerStatus.label(): String = when (this) {
    TimerStatus.IDLE -> "Ready"
    TimerStatus.PREPARING -> "Starting"
    TimerStatus.RUNNING -> "Live"
    TimerStatus.PAUSED -> "Paused"
    TimerStatus.FINISHED -> "Finished"
}
