package com.todayddl.app.notification

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.todayddl.app.MainActivity
import com.todayddl.app.R
import com.todayddl.app.data.local.dao.TaskDao
import com.todayddl.app.data.local.model.TaskWithTag
import com.todayddl.app.data.preferences.AppPreferencesRepository
import com.todayddl.app.domain.SemesterDateCalculator
import com.todayddl.app.domain.TaskStatusCalculator
import java.time.LocalDate

class OngoingNotificationHelper(
    private val context: Context,
    private val taskDao: TaskDao,
    private val preferencesRepository: AppPreferencesRepository
) {
    companion object {
        const val ONGOING_NOTIFICATION_ID = 1001
        private const val OPEN_APP_REQUEST_CODE = 100
        private const val DISMISS_REQUEST_CODE = 101
    }

    @Volatile
    private var lastPostedSignature: String? = null

    suspend fun refreshOngoingNotification(
        today: LocalDate = LocalDate.now(),
        force: Boolean = false
    ) {
        val prefs = preferencesRepository.getPreferencesOnce()
        val todayTasks = TaskStatusCalculator.sortTodayOngoingTasks(
            taskDao.getIncompleteTasksWithTagForDay(today.toEpochDay())
        )

        if (todayTasks.isEmpty() && prefs.hideNotificationWhenEmpty) {
            if (lastPostedSignature != null || isOngoingNotificationActive()) {
                NotificationManagerCompat.from(context).cancel(ONGOING_NOTIFICATION_ID)
                lastPostedSignature = null
            }
            return
        }

        if (!hasPostNotificationPermission()) {
            lastPostedSignature = null
            return
        }

        val signature = buildSignature(today.toEpochDay(), todayTasks)
        if (!force && signature == lastPostedSignature && isOngoingNotificationActive()) {
            return
        }

        NotificationChannels.ensureChannelsCreated(context)
        val notificationManager = NotificationManagerCompat.from(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            OPEN_APP_REQUEST_CODE,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, OngoingDismissedReceiver::class.java).apply {
            action = OngoingDismissedReceiver.ACTION_ONGOING_DISMISSED
        }
        val deletePendingIntent = PendingIntent.getBroadcast(
            context,
            DISMISS_REQUEST_CODE,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, NotificationChannels.TODAY_ONGOING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_ddl)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentPendingIntent)
            .setDeleteIntent(deletePendingIntent)

        if (todayTasks.isEmpty()) {
            builder
                .setContentTitle("今日暂无截止任务")
                .setContentText("点击打开 App 添加或查看 DDL")
        } else {
            val title = "今日 DDL（${todayTasks.size} 项待完成）"
            val first = todayTasks.first()
            val firstTimeTag = "[${SemesterDateCalculator.formatTimeMinutes(first.task.dueTimeMinutes)}]"
            val firstLine = "$firstTimeTag ${first.task.title}"

            val inboxStyle = NotificationCompat.InboxStyle()
                .setBigContentTitle(title)
            for (item in todayTasks) {
                val timeTag = "[${SemesterDateCalculator.formatTimeMinutes(item.task.dueTimeMinutes)}]"
                val tagSuffix = item.tag?.let { " · ${it.name}" } ?: ""
                inboxStyle.addLine("$timeTag ${item.task.title}$tagSuffix")
            }
            if (todayTasks.size > 5) {
                inboxStyle.setSummaryText("共 ${todayTasks.size} 项今日待完成")
            }

            builder
                .setContentTitle(title)
                .setContentText(firstLine)
                .setStyle(inboxStyle)
        }

        try {
            notificationManager.notify(ONGOING_NOTIFICATION_ID, builder.build())
            lastPostedSignature = signature
        } catch (_: SecurityException) {
            lastPostedSignature = null
        }
    }

    fun hasPostNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun isOngoingNotificationActive(): Boolean {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return false
        return manager.activeNotifications.any { it.id == ONGOING_NOTIFICATION_ID }
    }

    private fun buildSignature(todayEpochDay: Long, tasks: List<TaskWithTag>): String {
        if (tasks.isEmpty()) return "$todayEpochDay:empty"
        return buildString(tasks.size * 32) {
            append(todayEpochDay)
            for (item in tasks) {
                append('|')
                append(item.task.id)
                append(':')
                append(item.task.title)
                append(':')
                append(item.task.dueTimeMinutes)
                append(':')
                append(item.tag?.name)
            }
        }
    }
}
