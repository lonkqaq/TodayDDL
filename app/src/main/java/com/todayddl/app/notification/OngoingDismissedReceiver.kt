package com.todayddl.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.todayddl.app.TodayDdlApplication
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class OngoingDismissedReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_ONGOING_DISMISSED = "com.todayddl.app.action.ONGOING_DISMISSED"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? TodayDdlApplication ?: return
        val pendingResult = goAsync()

        app.appScope.launch {
            try {
                withTimeoutOrNull(8_000L) {
                    app.container.ongoingNotificationHelper.refreshOngoingNotification(force = true)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
