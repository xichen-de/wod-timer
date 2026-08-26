package dev.xichen.wodtimer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.xichen.wodtimer.preset.PresetMode
import dev.xichen.wodtimer.preset.Preset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigureScreen(viewModel: AppViewModel) {
    val preset by viewModel.draft.collectAsStateWithLifecycle()
    val validationMessage = preset.validationMessage()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Set up ${preset.mode.label()}") },
                navigationIcon = { IconButton(viewModel::back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (preset.mode) {
                PresetMode.FOR_TIME -> {
                    Text(
                        if (preset.forTimeCapEnabled) "Counts upward and finishes when the cap is reached."
                        else "Counts upward until you stop it.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Toggle("Time cap", preset.forTimeCapEnabled) { value ->
                        viewModel.updateDraft { it.copy(forTimeCapEnabled = value) }
                    }
                    if (preset.forTimeCapEnabled) {
                        DurationFields("Time cap", preset.durationMillis) {
                            viewModel.updateDraft { preset -> preset.copy(durationMillis = it) }
                        }
                    }
                }
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
            OutlinedTextField(
                value = preset.name,
                onValueChange = { value -> viewModel.updateDraft { it.copy(name = value) } },
                label = { Text("Preset name (for saving)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            validationMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = viewModel::startDraft,
                enabled = validationMessage == null,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) { Text("CONTINUE") }
            if (preset.id == 0L) {
                OutlinedButton(
                    onClick = viewModel::save,
                    enabled = validationMessage == null,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text("SAVE AS PRESET") }
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
    var text by remember { mutableStateOf(value.toString()) }
    var lastPushedValue by remember { mutableStateOf(value) }
    if (value != lastPushedValue) {
        text = value.toString()
        lastPushedValue = value
    }
    OutlinedTextField(
        value = text,
        onValueChange = { candidate ->
            if (candidate.all(Char::isDigit) && candidate.length <= 6) {
                text = candidate
                val parsed = candidate.toLongOrNull() ?: 0L
                lastPushedValue = parsed
                onChange(parsed)
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
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Switch) { onChecked(!checked) }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}

private fun Preset.validationMessage(): String? {
    if (preStartSeconds !in 0..60) return "Pre-start countdown must be between 0 and 60 seconds."
    return when (mode) {
        PresetMode.FOR_TIME -> when {
            forTimeCapEnabled && durationMillis !in 1_000L..24 * 60 * 60_000L -> "Time cap must be between 1 second and 24 hours."
            else -> null
        }
        PresetMode.AMRAP -> if (durationMillis !in 1_000L..24 * 60 * 60_000L) {
            "Duration must be between 1 second and 24 hours."
        } else null
        PresetMode.EVERY_X_MINUTES -> when {
            intervalMillis !in 1_000L..60 * 60_000L -> "Interval must be between 1 second and 60 minutes."
            rounds !in 1..999 -> "Rounds must be between 1 and 999."
            else -> null
        }
        PresetMode.INTERVALS -> when {
            workMillis !in 1_000L..60 * 60_000L -> "Work time must be between 1 second and 60 minutes."
            restMillis !in 0L..60 * 60_000L -> "Rest time must be between 0 and 60 minutes."
            rounds !in 1..999 -> "Rounds must be between 1 and 999."
            else -> null
        }
    }
}

private fun PresetMode.label() = when (this) {
    PresetMode.FOR_TIME -> "For Time"
    PresetMode.AMRAP -> "AMRAP"
    PresetMode.EVERY_X_MINUTES -> "Every X Minutes"
    PresetMode.INTERVALS -> "Intervals"
}
