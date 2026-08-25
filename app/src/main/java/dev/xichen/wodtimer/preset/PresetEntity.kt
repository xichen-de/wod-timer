package dev.xichen.wodtimer.preset

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "presets")
data class PresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val mode: String,
    val durationMillis: Long,
    val intervalMillis: Long,
    val workMillis: Long,
    val restMillis: Long,
    val rounds: Int,
    val preStartSeconds: Int,
    val soundEnabled: Boolean,
    val vibrationEnabled: Boolean,
    val warningEnabled: Boolean,
    @ColumnInfo(defaultValue = "0") val forTimeCapEnabled: Boolean,
    val position: Int,
)

fun PresetEntity.toDomain() = Preset(
    id, name, PresetMode.valueOf(mode), durationMillis, intervalMillis, workMillis,
    restMillis, rounds, preStartSeconds, soundEnabled, vibrationEnabled, warningEnabled,
    forTimeCapEnabled, position,
)

fun Preset.toEntity() = PresetEntity(
    id, name.trim(), mode.name, durationMillis, intervalMillis, workMillis,
    restMillis, rounds, preStartSeconds, soundEnabled, vibrationEnabled, warningEnabled,
    forTimeCapEnabled, position,
)
