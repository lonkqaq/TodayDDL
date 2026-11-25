package com.todayddl.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.todayddl.app.TodayDdlApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class OngoingDismissedReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_ONGOING_DISMISSED = "com.todayddl.app.action.ONGOING_DISMISSED"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? TodayDdlApplication ?: return
        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                app.container.ongoingNotificationHelper.refreshOngoingNotification()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
