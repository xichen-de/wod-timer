package dev.xichen.wodtimer.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.xichen.wodtimer.preset.Preset
import dev.xichen.wodtimer.preset.PresetMode
import dev.xichen.wodtimer.timer.formatClock
import java.time.LocalDate

@Composable
fun HomeScreen(viewModel: AppViewModel) {
    val presets by viewModel.presets.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var presetMenuOpen by remember { mutableStateOf(false) }
    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> if (uri != null) viewModel.backupPresets(uri) }
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) viewModel.restorePresets(uri) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surface)))
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, top = 26.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
        item {
            Text(
                "TRAINING TOOLS",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                letterSpacing = 1.8.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Ready when you are.",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                "Pick a format and get moving.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
            )
            SectionTitle("Quick start")
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickCard("For time", "Count up", Icons.Default.Timer, Modifier.weight(1f)) {
                        viewModel.configure(PresetMode.FOR_TIME)
                    }
                    QuickCard("AMRAP", "Beat the clock", Icons.Default.Bolt, Modifier.weight(1f)) {
                        viewModel.configure(PresetMode.AMRAP)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickCard("Every X", "Repeat rounds", Icons.Default.Repeat, Modifier.weight(1f)) {
                        viewModel.configure(PresetMode.EVERY_X_MINUTES)
                    }
                    QuickCard("Intervals", "Work + rest", Icons.Default.Whatshot, Modifier.weight(1f)) {
                        viewModel.configure(PresetMode.INTERVALS)
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(12.dp))
            SectionTitle("My presets", presets.size.takeIf { it > 0 }?.toString()) {
                Box {
                    IconButton(onClick = { presetMenuOpen = true }) {
                        Icon(Icons.Default.MoreVert, "Manage presets")
                    }
                    DropdownMenu(
                        expanded = presetMenuOpen,
                        onDismissRequest = { presetMenuOpen = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Back up presets") },
                            leadingIcon = { Icon(Icons.Default.FileUpload, null) },
                            enabled = presets.isNotEmpty(),
                            onClick = {
                                presetMenuOpen = false
                                backupLauncher.launch("wod-timer-presets-${LocalDate.now()}.json")
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Restore presets") },
                            leadingIcon = { Icon(Icons.Default.FileDownload, null) },
                            onClick = {
                                presetMenuOpen = false
                                restoreLauncher.launch(arrayOf("application/json", "text/json", "text/plain"))
                            },
                        )
                    }
                }
            }
            if (presets.isEmpty()) EmptyPresets()
        }
        itemsIndexed(presets, key = { _, preset -> preset.id }) { index, preset ->
            PresetCard(
                preset = preset,
                canMoveUp = index > 0,
                canMoveDown = index < presets.lastIndex,
                onLaunch = { viewModel.launch(preset) },
                onEdit = { viewModel.edit(preset) },
                onDuplicate = { viewModel.duplicate(preset) },
                onDelete = { viewModel.delete(preset) },
                onMove = { viewModel.move(preset, it) },
            )
        }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
        )
    }
}

@Composable
private fun SectionTitle(title: String, count: String? = null, action: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (count != null) {
            Surface(modifier = Modifier.padding(start = 9.dp), color = MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape) {
                Text(count, Modifier.padding(horizontal = 9.dp, vertical = 3.dp), style = MaterialTheme.typography.labelMedium)
            }
        }
        Spacer(Modifier.weight(1f))
        action()
    }
}

@Composable
private fun QuickCard(title: String, subtitle: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.height(132.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .55f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Surface(modifier = Modifier.size(42.dp), shape = RoundedCornerShape(13.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = .14f)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, Modifier.size(23.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun EmptyPresets() {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f)),
    ) {
        Text("Your saved workouts will live here.", Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PresetCard(
    preset: Preset,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onLaunch: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onMove: (Int) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(44.dp), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.secondary.copy(alpha = .13f)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Timer, null, Modifier.size(23.dp), tint = MaterialTheme.colorScheme.secondary)
                    }
                }
                Column(Modifier.weight(1f).padding(horizontal = 13.dp)) {
                    Text(preset.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(preset.summary(), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
                Surface(
                    modifier = Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onLaunch),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PlayArrow, "Launch ${preset.name}", Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                IconButton(onClick = { onMove(-1) }, enabled = canMoveUp, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.KeyboardArrowUp, "Move up", Modifier.size(21.dp))
                }
                IconButton(onClick = { onMove(1) }, enabled = canMoveDown, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.KeyboardArrowDown, "Move down", Modifier.size(21.dp))
                }
                Spacer(Modifier.weight(1f))
                Surface(modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onEdit), color = Color.Transparent) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Edit, null, Modifier.size(18.dp))
                        Text("Edit", Modifier.padding(start = 6.dp), style = MaterialTheme.typography.labelLarge)
                    }
                }
                Box {
                    IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.MoreVert, "More preset actions")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Duplicate") },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                            onClick = { menuOpen = false; onDuplicate() },
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            leadingIcon = { Icon(Icons.Default.Delete, null) },
                            onClick = { menuOpen = false; onDelete() },
                        )
                    }
                }
            }
        }
    }
}

private fun Preset.summary(): String = when (mode) {
    PresetMode.FOR_TIME -> "For Time · count up"
    PresetMode.AMRAP -> "AMRAP · ${formatClock(durationMillis)}"
    PresetMode.EVERY_X_MINUTES -> "Every ${formatClock(intervalMillis)} · $rounds rounds"
    PresetMode.INTERVALS -> "${workMillis / 1_000}s work · ${restMillis / 1_000}s rest · $rounds rounds"
}
