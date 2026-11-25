package com.todayddl.app.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicLong

class MidnightRefreshScheduler(private val context: Context) {

    companion object {
        const val ACTION_MIDNIGHT_REFRESH = "com.todayddl.app.action.MIDNIGHT_REFRESH"
        private const val REQUEST_CODE = 4001
    }

    private val lastScheduledTargetEpochDay = AtomicLong(Long.MIN_VALUE)

    /**
     * Schedules a non-wakeup [AlarmManager.RTC] alarm for the next day so the device
     * stays in deep sleep overnight and silently refreshes once naturally awake.
     */
    fun scheduleNextMidnightRefresh(
        today: LocalDate = LocalDate.now(),
        force: Boolean = false
    ) {
        val targetDate = today.plusDays(1)
        val targetEpochDay = targetDate.toEpochDay()
        if (!force && lastScheduledTargetEpochDay.get() == targetEpochDay) {
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val nextRefreshMillis = targetDate
            .atTime(0, 0, 5)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val pendingIntent = buildPendingIntent()
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC,
            nextRefreshMillis,
            pendingIntent
        )
        lastScheduledTargetEpochDay.set(targetEpochDay)
    }

    private fun buildPendingIntent(): PendingIntent {
        val intent = Intent(context, MidnightRefreshReceiver::class.java).apply {
            action = ACTION_MIDNIGHT_REFRESH
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
