package dev.xichen.wodtimer.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.xichen.wodtimer.WodTimerApplication
import dev.xichen.wodtimer.preset.Preset
import dev.xichen.wodtimer.preset.PresetBackup
import dev.xichen.wodtimer.preset.PresetMode
import dev.xichen.wodtimer.preset.defaultPreset
import dev.xichen.wodtimer.timer.TimerSnapshot
import dev.xichen.wodtimer.timer.TimerStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AppScreen { HOME, CONFIGURE, TIMER }

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as WodTimerApplication
    private val repository = app.presets
    private val controller = app.timerController

    val presets = repository.presets.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val timer: StateFlow<TimerSnapshot?> = controller.state
    private val _screen = MutableStateFlow(AppScreen.HOME)
    val screen = _screen.asStateFlow()
    private val _draft = MutableStateFlow(defaultPreset(PresetMode.AMRAP))
    val draft = _draft.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    fun configure(mode: PresetMode) {
        _draft.value = defaultPreset(mode)
        _screen.value = AppScreen.CONFIGURE
    }

    fun edit(preset: Preset) { _draft.value = preset; _screen.value = AppScreen.CONFIGURE }
    fun updateDraft(transform: (Preset) -> Preset) { _draft.value = transform(_draft.value) }

    fun save() = viewModelScope.launch {
        repository.save(_draft.value)
        _screen.value = AppScreen.HOME
    }

    fun launch(preset: Preset) {
        _draft.value = preset
        controller.prepare(preset.toTimerConfig())
        _screen.value = AppScreen.TIMER
    }

    fun startDraft() {
        val draft = _draft.value
        if (draft.id != 0L) viewModelScope.launch { repository.save(draft) }
        controller.prepare(draft.toTimerConfig())
        _screen.value = AppScreen.TIMER
    }
    fun duplicate(preset: Preset) = viewModelScope.launch { repository.duplicate(preset) }
    fun delete(preset: Preset) = viewModelScope.launch { repository.delete(preset) }
    fun move(preset: Preset, delta: Int) = viewModelScope.launch { repository.move(preset.id, delta) }

    fun backupPresets(uri: Uri) = viewModelScope.launch {
        val presetsToBackUp = presets.value
        val count = presetsToBackUp.size
        runCatching {
            withContext(Dispatchers.IO) {
                val resolver = getApplication<Application>().contentResolver
                resolver.openOutputStream(uri)?.bufferedWriter()?.let { writer ->
                    PresetBackup.write(presetsToBackUp, writer)
                } ?: error("The selected file could not be opened.")
            }
        }.onSuccess {
            _message.value = "Backed up $count preset${if (count == 1) "" else "s"}."
        }.onFailure { error ->
            _message.value = "Backup failed: ${error.message ?: "unknown error"}"
        }
    }

    fun restorePresets(uri: Uri) = viewModelScope.launch {
        runCatching {
            val restored = withContext(Dispatchers.IO) {
                val resolver = getApplication<Application>().contentResolver
                resolver.openInputStream(uri)?.bufferedReader()?.let(PresetBackup::read)
                    ?: error("The selected file could not be opened.")
            }
            repository.restore(restored)
            restored.size
        }.onSuccess { count ->
            _message.value = "Restored $count preset${if (count == 1) "" else "s"}."
        }.onFailure { error ->
            _message.value = "Restore failed: ${error.message ?: "invalid backup"}"
        }
    }

    fun messageShown() { _message.value = null }

    fun startTimer() = controller.start()
    fun pauseTimer() = controller.pause()
    fun resumeTimer() = controller.resume()
    fun stopTimer() = controller.stop()
    fun resetTimer() = controller.reset()

    fun back() {
        when (_screen.value) {
            AppScreen.HOME -> Unit
            AppScreen.CONFIGURE -> _screen.value = AppScreen.HOME
            AppScreen.TIMER -> {
                val status = timer.value?.status
                if (status == TimerStatus.IDLE || status == TimerStatus.FINISHED) _screen.value = AppScreen.HOME
            }
        }
    }
}
