package com.todayddl.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.todayddl.app.TodayDdlApplication
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class SystemEventReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in SUPPORTED_ACTIONS) return

        val app = context.applicationContext as? TodayDdlApplication ?: return
        val pendingResult = goAsync()

        app.appScope.launch {
            try {
                withTimeoutOrNull(8_000L) {
                    app.container.repository.restoreNotificationsAndAlarms()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private val SUPPORTED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED
        )
    }
}
