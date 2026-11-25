package com.todayddl.app.domain

import com.todayddl.app.data.local.entity.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ReminderCalculatorTest {

    private val dueDate = LocalDate.of(2026, 11, 25)

    private fun task(
        dueTimeMinutes: Int?,
        option: ReminderOption,
        isCompleted: Boolean = false
    ) = TaskEntity(
        id = 1L,
        title = "Test",
        dueDateEpochDay = dueDate.toEpochDay(),
        dueTimeMinutes = dueTimeMinutes,
        reminderOption = option.name,
        isCompleted = isCompleted,
        createdAtMillis = 1000L
    )

    @Test
    fun timedReminderOptions_computeExactReminderTimes() {
        val sixPm = 18 * 60

        assertEquals(
            LocalDateTime.of(2026, 11, 25, 18, 0),
            ReminderCalculator.computeReminderDateTime(task(sixPm, ReminderOption.AT_TIME))
        )
        assertEquals(
            LocalDateTime.of(2026, 11, 25, 17, 45),
            ReminderCalculator.computeReminderDateTime(task(sixPm, ReminderOption.BEFORE_15M))
        )
        assertEquals(
            LocalDateTime.of(2026, 11, 25, 17, 30),
            ReminderCalculator.computeReminderDateTime(task(sixPm, ReminderOption.BEFORE_30M))
        )
        assertEquals(
            LocalDateTime.of(2026, 11, 25, 17, 0),
            ReminderCalculator.computeReminderDateTime(task(sixPm, ReminderOption.BEFORE_1H))
        )
        assertEquals(
            LocalDateTime.of(2026, 11, 25, 15, 0),
            ReminderCalculator.computeReminderDateTime(task(sixPm, ReminderOption.BEFORE_3H))
        )
        assertEquals(
            LocalDateTime.of(2026, 11, 24, 18, 0),
            ReminderCalculator.computeReminderDateTime(task(sixPm, ReminderOption.BEFORE_1D))
        )
        assertNull(ReminderCalculator.computeReminderDateTime(task(sixPm, ReminderOption.NONE)))
    }

    @Test
    fun allDayReminderOptions_computeExactReminderTimes() {
        assertEquals(
            LocalDateTime.of(2026, 11, 25, 8, 0),
            ReminderCalculator.computeReminderDateTime(task(null, ReminderOption.SAME_DAY_0800))
        )
        assertEquals(
            LocalDateTime.of(2026, 11, 25, 12, 0),
            ReminderCalculator.computeReminderDateTime(task(null, ReminderOption.SAME_DAY_1200))
        )
        assertEquals(
            LocalDateTime.of(2026, 11, 24, 20, 0),
            ReminderCalculator.computeReminderDateTime(task(null, ReminderOption.PREV_DAY_2000))
        )
        assertNull(ReminderCalculator.computeReminderDateTime(task(null, ReminderOption.NONE)))
    }

    @Test
    fun completedOrIncompatibleOptions_returnNullAndAdaptCorrectly() {
        assertNull(
            ReminderCalculator.computeReminderDateTime(
                task(18 * 60, ReminderOption.BEFORE_1H, isCompleted = true)
            )
        )
        assertNull(
            ReminderCalculator.computeReminderDateTime(
                task(null, ReminderOption.BEFORE_1H)
            )
        )

        assertEquals(
            ReminderOption.BEFORE_1H,
            ReminderOption.adaptForTimeMode(ReminderOption.SAME_DAY_0800, hasExactTime = true)
        )
        assertEquals(
            ReminderOption.SAME_DAY_0800,
            ReminderOption.adaptForTimeMode(ReminderOption.BEFORE_30M, hasExactTime = false)
        )
        assertEquals(
            ReminderOption.NONE,
            ReminderOption.adaptForTimeMode(ReminderOption.NONE, hasExactTime = true)
        )
    }
}
