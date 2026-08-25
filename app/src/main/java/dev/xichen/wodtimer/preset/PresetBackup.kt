package dev.xichen.wodtimer.preset

import android.util.JsonReader
import android.util.JsonWriter
import java.io.Reader
import java.io.Writer

object PresetBackup {
    private const val FORMAT = "wod-timer-presets"
    private const val VERSION = 1
    private const val MAX_PRESETS = 10_000

    fun write(presets: List<Preset>, destination: Writer) {
        JsonWriter(destination).use { json ->
            json.setIndent("  ")
            json.beginObject()
            json.name("format").value(FORMAT)
            json.name("version").value(VERSION.toLong())
            json.name("presets").beginArray()
            presets.forEach { preset ->
                val clean = preset.normalized()
                json.beginObject()
                json.name("name").value(clean.name)
                json.name("mode").value(clean.mode.name)
                json.name("durationMillis").value(clean.durationMillis)
                json.name("intervalMillis").value(clean.intervalMillis)
                json.name("workMillis").value(clean.workMillis)
                json.name("restMillis").value(clean.restMillis)
                json.name("rounds").value(clean.rounds.toLong())
                json.name("preStartSeconds").value(clean.preStartSeconds.toLong())
                json.name("soundEnabled").value(clean.soundEnabled)
                json.name("vibrationEnabled").value(clean.vibrationEnabled)
                json.name("warningEnabled").value(clean.warningEnabled)
                json.name("forTimeCapEnabled").value(clean.forTimeCapEnabled)
                json.endObject()
            }
            json.endArray()
            json.endObject()
        }
    }

    fun read(source: Reader): List<Preset> {
        var format: String? = null
        var version: Int? = null
        var presets: List<Preset>? = null
        JsonReader(source).use { json ->
            json.beginObject()
            while (json.hasNext()) {
                when (json.nextName()) {
                    "format" -> format = json.nextString()
                    "version" -> version = json.nextInt()
                    "presets" -> presets = readPresets(json)
                    else -> json.skipValue()
                }
            }
            json.endObject()
        }
        require(format == FORMAT) { "This is not a WOD Timer preset backup." }
        require(version == VERSION) { "Unsupported backup version: ${version ?: "missing"}." }
        return requireNotNull(presets) { "The backup does not contain presets." }
    }

    private fun readPresets(json: JsonReader): List<Preset> = buildList {
        json.beginArray()
        while (json.hasNext()) {
            require(size < MAX_PRESETS) { "The backup contains too many presets." }
            add(readPreset(json))
        }
        json.endArray()
    }

    private fun readPreset(json: JsonReader): Preset {
        var name: String? = null
        var mode: PresetMode? = null
        var durationMillis = 10 * 60_000L
        var intervalMillis = 60_000L
        var workMillis = 20_000L
        var restMillis = 10_000L
        var rounds = 8
        var preStartSeconds = 3
        var soundEnabled = true
        var vibrationEnabled = true
        var warningEnabled = true
        var forTimeCapEnabled = false

        json.beginObject()
        while (json.hasNext()) {
            when (json.nextName()) {
                "name" -> name = json.nextString()
                "mode" -> mode = runCatching { PresetMode.valueOf(json.nextString()) }
                    .getOrElse { throw IllegalArgumentException("The backup contains an unknown timer mode.") }
                "durationMillis" -> durationMillis = json.nextLong()
                "intervalMillis" -> intervalMillis = json.nextLong()
                "workMillis" -> workMillis = json.nextLong()
                "restMillis" -> restMillis = json.nextLong()
                "rounds" -> rounds = json.nextInt()
                "preStartSeconds" -> preStartSeconds = json.nextInt()
                "soundEnabled" -> soundEnabled = json.nextBoolean()
                "vibrationEnabled" -> vibrationEnabled = json.nextBoolean()
                "warningEnabled" -> warningEnabled = json.nextBoolean()
                "forTimeCapEnabled" -> forTimeCapEnabled = json.nextBoolean()
                else -> json.skipValue()
            }
        }
        json.endObject()

        require(name != null && name.length <= 200) { "A preset has an invalid name." }
        return Preset(
            name = name,
            mode = requireNotNull(mode) { "A preset is missing its timer mode." },
            durationMillis = durationMillis,
            intervalMillis = intervalMillis,
            workMillis = workMillis,
            restMillis = restMillis,
            rounds = rounds,
            preStartSeconds = preStartSeconds,
            soundEnabled = soundEnabled,
            vibrationEnabled = vibrationEnabled,
            warningEnabled = warningEnabled,
            forTimeCapEnabled = forTimeCapEnabled,
        ).normalized()
    }
}
