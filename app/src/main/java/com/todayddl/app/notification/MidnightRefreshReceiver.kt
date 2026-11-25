package com.todayddl.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.todayddl.app.TodayDdlApplication
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class MidnightRefreshReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? TodayDdlApplication ?: return
        val pendingResult = goAsync()

        app.appScope.launch {
            try {
                withTimeoutOrNull(8_000L) {
                    app.container.ongoingNotificationHelper.refreshOngoingNotification()
                    app.container.midnightRefreshScheduler.scheduleNextMidnightRefresh()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
