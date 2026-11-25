package com.todayddl.app

import android.app.Application
import com.todayddl.app.di.AppContainer
import com.todayddl.app.notification.NotificationChannels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TodayDdlApplication : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.ensureChannelsCreated(this)
        container = AppContainer(this)
        appScope.launch {
            container.repository.initializeDefaults()
        }
    }
}
