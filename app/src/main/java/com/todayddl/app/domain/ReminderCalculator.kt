package com.todayddl.app.domain

import com.todayddl.app.data.local.entity.TaskEntity
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object ReminderCalculator {

    fun computeReminderDateTime(task: TaskEntity): LocalDateTime? {
        if (task.isCompleted) return null

        val option = ReminderOption.entries.firstOrNull { it.name == task.reminderOption } ?: return null
        if (option == ReminderOption.NONE) return null

        val dueDate = LocalDate.ofEpochDay(task.dueDateEpochDay)
        val exactMinutes = task.dueTimeMinutes
        val hasExactTime = exactMinutes != null
        if (!option.isCompatibleWith(hasExactTime)) return null

        return if (exactMinutes != null) {
            val dueDateTime = dueDate.atTime(exactMinutes / 60, exactMinutes % 60)
            when (option) {
                ReminderOption.AT_TIME -> dueDateTime
                ReminderOption.BEFORE_15M -> dueDateTime.minusMinutes(15)
                ReminderOption.BEFORE_30M -> dueDateTime.minusMinutes(30)
                ReminderOption.BEFORE_1H -> dueDateTime.minusHours(1)
                ReminderOption.BEFORE_3H -> dueDateTime.minusHours(3)
                ReminderOption.BEFORE_1D -> dueDateTime.minusDays(1)
                else -> null
            }
        } else {
            when (option) {
                ReminderOption.SAME_DAY_0800 -> dueDate.atTime(8, 0)
                ReminderOption.SAME_DAY_1200 -> dueDate.atTime(12, 0)
                ReminderOption.PREV_DAY_2000 -> dueDate.minusDays(1).atTime(20, 0)
                else -> null
            }
        }
    }

    fun computeReminderInstant(
        task: TaskEntity,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Instant? = computeReminderDateTime(task)?.atZone(zoneId)?.toInstant()

    fun buildReminderMessage(task: TaskEntity, semesterStartMonday: LocalDate): String {
        val dueDate = LocalDate.ofEpochDay(task.dueDateEpochDay)
        val schedule = SemesterDateCalculator.formatTaskDueSchedule(
            dueDate = dueDate,
            dueTimeMinutes = task.dueTimeMinutes,
            semesterStartMonday = semesterStartMonday,
            referenceYear = dueDate.year
        )
        return "截止时间：$schedule"
    }
}
