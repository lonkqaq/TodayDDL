package com.todayddl.app.di

import android.content.Context
import com.todayddl.app.data.local.AppDatabase
import com.todayddl.app.data.preferences.AppPreferencesRepository
import com.todayddl.app.data.repository.DdlRepository
import com.todayddl.app.notification.MidnightRefreshScheduler
import com.todayddl.app.notification.OngoingNotificationHelper
import com.todayddl.app.notification.ReminderScheduler

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: AppDatabase by lazy {
        AppDatabase.build(appContext)
    }

    val preferencesRepository: AppPreferencesRepository by lazy {
        AppPreferencesRepository(appContext)
    }

    val ongoingNotificationHelper: OngoingNotificationHelper by lazy {
        OngoingNotificationHelper(
            context = appContext,
            taskDao = database.taskDao(),
            preferencesRepository = preferencesRepository
        )
    }

    val reminderScheduler: ReminderScheduler by lazy {
        ReminderScheduler(appContext)
    }

    val midnightRefreshScheduler: MidnightRefreshScheduler by lazy {
        MidnightRefreshScheduler(appContext)
    }

    val repository: DdlRepository by lazy {
        DdlRepository(
            taskDao = database.taskDao(),
            tagDao = database.tagDao(),
            preferencesRepository = preferencesRepository,
            ongoingNotificationHelper = ongoingNotificationHelper,
            reminderScheduler = reminderScheduler,
            midnightRefreshScheduler = midnightRefreshScheduler
        )
    }
}
