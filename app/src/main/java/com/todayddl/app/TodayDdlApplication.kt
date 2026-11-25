package com.todayddl.app

import android.app.Application
import com.todayddl.app.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class TodayDdlApplication : Application() {

    lateinit var container: AppContainer
        private set

    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
