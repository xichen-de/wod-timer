package dev.xichen.wodtimer.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dev.xichen.wodtimer.MainActivity
import dev.xichen.wodtimer.WodTimerApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class TimerService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observation: Job? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val controller = (application as WodTimerApplication).timerController
        val initial = controller.state.value
        startForeground(NOTIFICATION_ID, notification(initial))
        observation?.cancel()
        observation = scope.launch {
            var lastKey: String? = null
            controller.state.filterNotNull().collect { state ->
                val key = notificationText(state)
                if (key != lastKey) {
                    lastKey = key
                    getSystemService(NotificationManager::class.java)
                        .notify(NOTIFICATION_ID, notification(state))
                }
                if (state.status == TimerStatus.IDLE || state.status == TimerStatus.FINISHED) stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() { scope.cancel(); super.onDestroy() }

    private fun createChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Active timer", NotificationManager.IMPORTANCE_LOW),
        )
    }

    private fun notification(state: TimerSnapshot?): Notification {
        val launch = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val text = state?.let(::notificationText) ?: "Timer active"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(state?.config?.workoutName ?: state?.config?.mode?.title() ?: "WOD Timer")
            .setContentText(text)
            .setContentIntent(launch)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "active_timer"
        private const val NOTIFICATION_ID = 42
    }
}

internal fun notificationText(state: TimerSnapshot): String {
    val label = if (state.status == TimerStatus.PAUSED) "PAUSED" else state.phase.name
    val clock = when {
        state.phase == TimerPhase.PREPARING -> formatClock((state.preStartRemainingSeconds ?: 0) * 1_000L)
        state.config.mode is TimerMode.ForTime -> formatElapsed(state.elapsedMillis)
        else -> formatClock(state.intervalRemainingMillis ?: state.remainingMillis ?: state.elapsedMillis)
    }
    return "$label · $clock"
}
