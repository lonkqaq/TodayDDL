package com.todayddl.app.data.preferences

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "today_ddl_prefs")

@Immutable
data class AppPreferences(
    val semesterStartMondayEpochDay: Long,
    val hideNotificationWhenEmpty: Boolean
)

class AppPreferencesRepository(private val context: Context) {

    private object Keys {
        val SEMESTER_START_MONDAY_EPOCH_DAY = longPreferencesKey("semester_start_monday_epoch_day")
        val HIDE_NOTIFICATION_WHEN_EMPTY = booleanPreferencesKey("hide_notification_when_empty")
        val INITIAL_TAGS_SEEDED = booleanPreferencesKey("initial_tags_seeded")
    }

    val preferencesFlow: Flow<AppPreferences> = context.appDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            AppPreferences(
                semesterStartMondayEpochDay = prefs[Keys.SEMESTER_START_MONDAY_EPOCH_DAY]
                    ?: defaultStartMondayEpochDay(LocalDate.now()),
                hideNotificationWhenEmpty = prefs[Keys.HIDE_NOTIFICATION_WHEN_EMPTY] ?: false
            )
        }

    suspend fun ensureInitialized(today: LocalDate = LocalDate.now()) {
        val current = context.appDataStore.data
            .catch { exception ->
                if (exception is IOException) emit(emptyPreferences()) else throw exception
            }
            .first()
        if (current[Keys.SEMESTER_START_MONDAY_EPOCH_DAY] != null &&
            current[Keys.HIDE_NOTIFICATION_WHEN_EMPTY] != null
        ) {
            return
        }
        context.appDataStore.edit { prefs ->
            if (prefs[Keys.SEMESTER_START_MONDAY_EPOCH_DAY] == null) {
                prefs[Keys.SEMESTER_START_MONDAY_EPOCH_DAY] = defaultStartMondayEpochDay(today)
            }
            if (prefs[Keys.HIDE_NOTIFICATION_WHEN_EMPTY] == null) {
                prefs[Keys.HIDE_NOTIFICATION_WHEN_EMPTY] = false
            }
        }
    }

    suspend fun getPreferencesOnce(): AppPreferences = preferencesFlow.first()

    suspend fun setSemesterStartMonday(date: LocalDate) {
        val alignedMonday = alignToMonday(date)
        context.appDataStore.edit { prefs ->
            prefs[Keys.SEMESTER_START_MONDAY_EPOCH_DAY] = alignedMonday.toEpochDay()
        }
    }

    suspend fun setHideNotificationWhenEmpty(hide: Boolean) {
        context.appDataStore.edit { prefs ->
            prefs[Keys.HIDE_NOTIFICATION_WHEN_EMPTY] = hide
        }
    }

    suspend fun isInitialTagsSeeded(): Boolean {
        val prefs = context.appDataStore.data
            .catch { exception ->
                if (exception is IOException) emit(emptyPreferences()) else throw exception
            }
            .first()
        return prefs[Keys.INITIAL_TAGS_SEEDED] ?: false
    }

    suspend fun markInitialTagsSeeded() {
        context.appDataStore.edit { prefs ->
            prefs[Keys.INITIAL_TAGS_SEEDED] = true
        }
    }

    private fun defaultStartMondayEpochDay(today: LocalDate): Long =
        alignToMonday(today).toEpochDay()

    private fun alignToMonday(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
}
