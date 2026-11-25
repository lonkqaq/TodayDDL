package com.todayddl.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.todayddl.app.TodayDdlApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate

class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(ReminderScheduler.EXTRA_TASK_ID, -1L)
        if (taskId <= 0L) return

        val app = context.applicationContext as? TodayDdlApplication ?: return
        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val container = app.container
                val taskWithTag = container.repository.getTaskWithTagById(taskId) ?: return@launch
                if (taskWithTag.task.isCompleted) return@launch

                val prefs = container.preferencesRepository.getPreferencesOnce()
                val semesterStartMonday = LocalDate.ofEpochDay(prefs.semesterStartMondayEpochDay)
                container.reminderScheduler.showReminderNotification(
                    task = taskWithTag.task,
                    tagName = taskWithTag.tag?.name,
                    semesterStartMonday = semesterStartMonday
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
