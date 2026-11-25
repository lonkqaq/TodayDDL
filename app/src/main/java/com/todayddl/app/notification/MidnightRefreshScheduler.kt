package com.todayddl.app.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDate
import java.time.ZoneId

class MidnightRefreshScheduler(private val context: Context) {

    companion object {
        const val ACTION_MIDNIGHT_REFRESH = "com.todayddl.app.action.MIDNIGHT_REFRESH"
        private const val REQUEST_CODE = 4001
    }

    fun scheduleNextMidnightRefresh(today: LocalDate = LocalDate.now()) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val nextRefreshMillis = today
            .plusDays(1)
            .atTime(0, 0, 5)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val pendingIntent = buildPendingIntent()
        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        if (canExact) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextRefreshMillis,
                    pendingIntent
                )
            } catch (_: SecurityException) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextRefreshMillis,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextRefreshMillis,
                pendingIntent
            )
        }
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
