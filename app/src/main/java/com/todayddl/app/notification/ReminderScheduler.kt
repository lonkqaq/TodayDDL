package com.todayddl.app.notification

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.todayddl.app.MainActivity
import com.todayddl.app.R
import com.todayddl.app.data.local.entity.TaskEntity
import com.todayddl.app.domain.ReminderCalculator
import java.time.LocalDate

class ReminderScheduler(private val context: Context) {

    companion object {
        const val ACTION_TRIGGER_REMINDER = "com.todayddl.app.action.TRIGGER_REMINDER"
        const val EXTRA_TASK_ID = "extra_task_id"
        private const val REMINDER_NOTIFICATION_BASE_ID = 20000
        private const val TEST_REMINDER_NOTIFICATION_ID = 29999
        private const val OPEN_APP_REQUEST_CODE = 300
    }

    private val alarmManager: AlarmManager?
        get() = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    fun canScheduleExactAlarms(): Boolean {
        val manager = alarmManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            manager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun scheduleReminder(task: TaskEntity) {
        cancelReminder(task.id)
        val triggerInstant = ReminderCalculator.computeReminderInstant(task) ?: return
        val triggerMillis = triggerInstant.toEpochMilli()
        if (triggerMillis <= System.currentTimeMillis()) return

        val manager = alarmManager ?: return
        val pendingIntent = buildAlarmPendingIntent(task.id)
        if (canScheduleExactAlarms()) {
            try {
                manager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            } catch (_: SecurityException) {
                manager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            }
        } else {
            manager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        }
    }

    fun cancelReminder(taskId: Long) {
        if (taskId <= 0L) return
        val manager = alarmManager ?: return
        manager.cancel(buildAlarmPendingIntent(taskId))
    }

    fun rescheduleAll(tasks: List<TaskEntity>) {
        for (task in tasks) {
            if (!task.isCompleted) {
                scheduleReminder(task)
            } else {
                cancelReminder(task.id)
            }
        }
    }

    fun showReminderNotification(
        task: TaskEntity,
        tagName: String?,
        semesterStartMonday: LocalDate
    ): Boolean {
        NotificationChannels.ensureChannelsCreated(context)
        if (!hasNotificationPermission()) return false

        val scheduleText = ReminderCalculator.buildReminderMessage(task, semesterStartMonday)
        val tagPrefix = tagName?.let { "[$it] " } ?: ""
        val contentText = "$tagPrefix$scheduleText"

        val notificationId = REMINDER_NOTIFICATION_BASE_ID + (task.id % 9000).toInt()
        return dispatchHighPriorityNotification(
            notificationId = notificationId,
            title = "DDL 提醒：${task.title}",
            body = contentText
        )
    }

    fun sendImmediateTestReminder(): Boolean {
        NotificationChannels.ensureChannelsCreated(context)
        if (!hasNotificationPermission()) return false

        return dispatchHighPriorityNotification(
            notificationId = TEST_REMINDER_NOTIFICATION_ID,
            title = "DDL 测试提醒",
            body = "提醒渠道正常，点击可直达 TodayDDL 主界面。"
        )
    }

    private fun dispatchHighPriorityNotification(
        notificationId: Int,
        title: String,
        body: String
    ): Boolean {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            OPEN_APP_REQUEST_CODE,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.DDL_REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_ddl)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    private fun buildAlarmPendingIntent(taskId: Long): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_REMINDER
            data = Uri.parse("todayddl://reminder/$taskId")
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val requestCode = (taskId and 0x7FFFFFFF).toInt()
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}
