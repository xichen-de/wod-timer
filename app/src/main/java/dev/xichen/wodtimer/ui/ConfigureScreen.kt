package dev.xichen.wodtimer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.xichen.wodtimer.preset.PresetMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigureScreen(viewModel: AppViewModel) {
    val preset by viewModel.draft.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configure ${preset.mode.label()}") },
                navigationIcon = { IconButton(viewModel::back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = preset.name,
                onValueChange = { value -> viewModel.updateDraft { it.copy(name = value) } },
                label = { Text("Preset name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            when (preset.mode) {
                PresetMode.FOR_TIME -> Text("Counts upward until you stop it.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                PresetMode.AMRAP -> DurationFields("Duration", preset.durationMillis) {
                    viewModel.updateDraft { preset -> preset.copy(durationMillis = it) }
                }
                PresetMode.EVERY_X_MINUTES -> {
                    DurationFields("Interval", preset.intervalMillis) {
                        viewModel.updateDraft { preset -> preset.copy(intervalMillis = it) }
                    }
                    IntegerField("Rounds", preset.rounds) { value -> viewModel.updateDraft { it.copy(rounds = value) } }
                }
                PresetMode.INTERVALS -> {
                    SecondsField("Work seconds", preset.workMillis) {
                        viewModel.updateDraft { preset -> preset.copy(workMillis = it) }
                    }
                    SecondsField("Rest seconds", preset.restMillis) {
                        viewModel.updateDraft { preset -> preset.copy(restMillis = it) }
                    }
                    IntegerField("Rounds", preset.rounds) { value -> viewModel.updateDraft { it.copy(rounds = value) } }
                }
            }
            IntegerField("Pre-start countdown (seconds)", preset.preStartSeconds) { value ->
                viewModel.updateDraft { it.copy(preStartSeconds = value) }
            }
            Toggle("Sound cues", preset.soundEnabled) { value -> viewModel.updateDraft { it.copy(soundEnabled = value) } }
            Toggle("Vibration", preset.vibrationEnabled) { value -> viewModel.updateDraft { it.copy(vibrationEnabled = value) } }
            Toggle("Final-seconds warning", preset.warningEnabled) { value -> viewModel.updateDraft { it.copy(warningEnabled = value) } }
            Spacer(Modifier.height(4.dp))
            Button(onClick = viewModel::startDraft, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("START") }
            OutlinedButton(onClick = viewModel::save, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(if (preset.id == 0L) "SAVE AS PRESET" else "SAVE CHANGES")
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun DurationFields(label: String, millis: Long, onChange: (Long) -> Unit) {
    Text(label, style = MaterialTheme.typography.titleMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        NumericField("Minutes", millis / 60_000L, Modifier.weight(1f)) { minutes ->
            onChange(minutes * 60_000L + (millis / 1_000L % 60) * 1_000L)
        }
        NumericField("Seconds", millis / 1_000L % 60, Modifier.weight(1f)) { seconds ->
            onChange((millis / 60_000L) * 60_000L + seconds.coerceIn(0, 59) * 1_000L)
        }
    }
}

@Composable
private fun SecondsField(label: String, millis: Long, onChange: (Long) -> Unit) {
    NumericField(label, millis / 1_000L, Modifier.fillMaxWidth()) { onChange(it * 1_000L) }
}

@Composable
private fun IntegerField(label: String, value: Int, onChange: (Int) -> Unit) {
    NumericField(label, value.toLong(), Modifier.fillMaxWidth()) { onChange(it.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()) }
}

@Composable
private fun NumericField(label: String, value: Long, modifier: Modifier, onChange: (Long) -> Unit) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = { candidate ->
            if (candidate.all(Char::isDigit) && candidate.length <= 6) {
                text = candidate
                onChange(candidate.toLongOrNull() ?: 0L)
            }
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}

@Composable
private fun Toggle(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

private fun PresetMode.label() = when (this) {
    PresetMode.FOR_TIME -> "For Time"
    PresetMode.AMRAP -> "AMRAP"
    PresetMode.EVERY_X_MINUTES -> "Every X Minutes"
    PresetMode.INTERVALS -> "Intervals"
}
