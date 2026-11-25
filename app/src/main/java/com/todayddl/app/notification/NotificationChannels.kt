package com.todayddl.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object NotificationChannels {
    const val TODAY_ONGOING_CHANNEL_ID = "today_ongoing_channel"
    const val DDL_REMINDER_CHANNEL_ID = "ddl_reminder_channel"

    fun ensureChannelsCreated(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val ongoingChannel = NotificationChannel(
            TODAY_ONGOING_CHANNEL_ID,
            "今日 DDL 常驻通知",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "在状态栏与通知栏静默常驻展示今日截止的待办任务"
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
            setSound(null, null)
        }

        val reminderChannel = NotificationChannel(
            DDL_REMINDER_CHANNEL_ID,
            "DDL 截止提醒",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "在任务截止前发送弹窗横幅、响铃与震动提醒"
            enableLights(true)
            enableVibration(true)
        }

        manager.createNotificationChannel(ongoingChannel)
        manager.createNotificationChannel(reminderChannel)
    }
}
