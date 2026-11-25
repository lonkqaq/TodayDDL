package com.todayddl.app.data.repository

import com.todayddl.app.data.local.dao.TagDao
import com.todayddl.app.data.local.dao.TaskDao
import com.todayddl.app.data.local.entity.TagEntity
import com.todayddl.app.data.local.entity.TaskEntity
import com.todayddl.app.data.local.model.TaskWithTag
import com.todayddl.app.data.preferences.AppPreferences
import com.todayddl.app.data.preferences.AppPreferencesRepository
import com.todayddl.app.domain.SemesterDateCalculator
import com.todayddl.app.notification.MidnightRefreshScheduler
import com.todayddl.app.notification.OngoingNotificationHelper
import com.todayddl.app.notification.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicBoolean

class DdlRepository(
    private val taskDao: TaskDao,
    private val tagDao: TagDao,
    private val preferencesRepository: AppPreferencesRepository,
    private val ongoingNotificationHelper: OngoingNotificationHelper,
    private val reminderScheduler: ReminderScheduler,
    private val midnightRefreshScheduler: MidnightRefreshScheduler
) {
    private val initialized = AtomicBoolean(false)

    val tasksWithTagFlow: Flow<List<TaskWithTag>> = taskDao.observeAllTasksWithTag()
    val tagsFlow: Flow<List<TagEntity>> = tagDao.observeAllTags()
    val preferencesFlow: Flow<AppPreferences> = preferencesRepository.preferencesFlow

    suspend fun initializeDefaults() {
        if (!initialized.compareAndSet(false, true)) return
        preferencesRepository.ensureInitialized()
        if (!preferencesRepository.isInitialTagsSeeded()) {
            if (tagDao.getAllTagsOnce().isEmpty()) {
                tagDao.insertAll(TagEntity.defaultTags())
            }
            preferencesRepository.markInitialTagsSeeded()
        }
        restoreNotificationsAndAlarms()
    }

    suspend fun restoreNotificationsAndAlarms() {
        ongoingNotificationHelper.refreshOngoingNotification(force = true)
        midnightRefreshScheduler.scheduleNextMidnightRefresh(force = true)
        reminderScheduler.rescheduleAll(taskDao.getIncompleteTasksOnce())
    }

    suspend fun refreshOngoingAndMidnightOnly(force: Boolean = false) {
        ongoingNotificationHelper.refreshOngoingNotification(force = force)
        midnightRefreshScheduler.scheduleNextMidnightRefresh(force = force)
    }

    suspend fun getTaskWithTagById(id: Long): TaskWithTag? = taskDao.getTaskWithTagById(id)

    suspend fun getTaskById(id: Long): TaskEntity? = taskDao.getTaskById(id)

    suspend fun upsertTask(task: TaskEntity): Long {
        val savedId = if (task.id == 0L) {
            taskDao.insert(task)
        } else {
            taskDao.update(task)
            task.id
        }
        val persistedTask = task.copy(id = savedId)
        if (persistedTask.isCompleted) {
            reminderScheduler.cancelReminder(savedId)
        } else {
            reminderScheduler.scheduleReminder(persistedTask)
        }
        ongoingNotificationHelper.refreshOngoingNotification()
        return savedId
    }

    suspend fun toggleTaskCompletion(task: TaskEntity) {
        val nextCompleted = !task.isCompleted
        val completedAt = if (nextCompleted) System.currentTimeMillis() else null
        taskDao.updateCompletion(task.id, nextCompleted, completedAt)
        val updated = task.copy(isCompleted = nextCompleted, completedAtMillis = completedAt)
        if (nextCompleted) {
            reminderScheduler.cancelReminder(task.id)
        } else {
            reminderScheduler.scheduleReminder(updated)
        }
        ongoingNotificationHelper.refreshOngoingNotification()
    }

    suspend fun deleteTask(taskId: Long) {
        taskDao.deleteById(taskId)
        reminderScheduler.cancelReminder(taskId)
        ongoingNotificationHelper.refreshOngoingNotification()
    }

    suspend fun addTag(name: String, colorHex: Long): Long {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return -1L
        return tagDao.insert(TagEntity(name = trimmed, colorHex = colorHex))
    }

    suspend fun updateTag(tag: TagEntity) {
        val trimmed = tag.name.trim()
        if (trimmed.isEmpty()) return
        tagDao.update(tag.copy(name = trimmed))
        ongoingNotificationHelper.refreshOngoingNotification()
    }

    suspend fun deleteTag(tag: TagEntity) {
        tagDao.delete(tag)
        ongoingNotificationHelper.refreshOngoingNotification()
    }

    suspend fun updateSemesterStartDate(pickedDate: LocalDate): LocalDate {
        val alignedMonday = SemesterDateCalculator.alignToMonday(pickedDate)
        preferencesRepository.setSemesterStartMonday(alignedMonday)
        ongoingNotificationHelper.refreshOngoingNotification()
        return alignedMonday
    }

    suspend fun setHideNotificationWhenEmpty(hide: Boolean) {
        preferencesRepository.setHideNotificationWhenEmpty(hide)
        ongoingNotificationHelper.refreshOngoingNotification()
    }

    fun sendTestReminder(): Boolean = reminderScheduler.sendImmediateTestReminder()

    fun canScheduleExactAlarms(): Boolean = reminderScheduler.canScheduleExactAlarms()

    fun hasPostNotificationPermission(): Boolean = ongoingNotificationHelper.hasPostNotificationPermission()
}
