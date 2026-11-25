package com.todayddl.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.todayddl.app.TodayDdlApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MidnightRefreshReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? TodayDdlApplication ?: return
        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                app.container.ongoingNotificationHelper.refreshOngoingNotification()
                app.container.midnightRefreshScheduler.scheduleNextMidnightRefresh()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
