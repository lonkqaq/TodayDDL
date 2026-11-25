package com.todayddl.app.domain

import com.todayddl.app.data.local.entity.TagEntity
import com.todayddl.app.data.local.entity.TaskEntity
import com.todayddl.app.data.local.model.TaskWithTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class TaskStatusCalculatorTest {

    private val today = LocalDate.of(2026, 11, 25)

    private fun makeTask(
        id: Long = 1L,
        title: String = "任务$id",
        dueDate: LocalDate = today,
        dueTimeMinutes: Int? = null,
        tagId: Long? = null,
        isCompleted: Boolean = false,
        createdAtMillis: Long = id * 1000L,
        completedAtMillis: Long? = null
    ) = TaskEntity(
        id = id,
        title = title,
        dueDateEpochDay = dueDate.toEpochDay(),
        dueTimeMinutes = dueTimeMinutes,
        tagId = tagId,
        isCompleted = isCompleted,
        createdAtMillis = createdAtMillis,
        completedAtMillis = completedAtMillis
    )

    @Test
    fun allDayTask_overdueStartsAtNextDayMidnightNot2359() {
        val allDayToday = makeTask(dueDate = today, dueTimeMinutes = null)

        val endOfToday = LocalDateTime.of(2026, 11, 25, 23, 59, 59)
        assertFalse(TaskStatusCalculator.isOverdue(allDayToday, endOfToday))
        val statusEndOfToday = TaskStatusCalculator.evaluate(allDayToday, endOfToday)
        assertEquals(TaskGroup.TODAY, statusEndOfToday.group)
        assertEquals("今日全天", statusEndOfToday.countdownText)
        assertEquals(UrgencyLevel.URGENT_ORANGE, statusEndOfToday.urgencyLevel)

        val startOfNextDay = LocalDateTime.of(2026, 11, 26, 0, 0, 0)
        assertTrue(TaskStatusCalculator.isOverdue(allDayToday, startOfNextDay))
        val statusNextDay = TaskStatusCalculator.evaluate(allDayToday, startOfNextDay)
        assertEquals(TaskGroup.OVERDUE, statusNextDay.group)
        assertEquals("已逾期 1 天", statusNextDay.countdownText)
        assertEquals(UrgencyLevel.OVERDUE_RED, statusNextDay.urgencyLevel)
    }

    @Test
    fun allDayTask_upcomingCountdownAndUrgencyTiers() {
        val now = LocalDateTime.of(2026, 11, 25, 14, 0)

        val in2Days = makeTask(dueDate = today.plusDays(2), dueTimeMinutes = null)
        val status2Days = TaskStatusCalculator.evaluate(in2Days, now)
        assertEquals(TaskGroup.UPCOMING, status2Days.group)
        assertEquals("剩 2 天", status2Days.countdownText)
        assertEquals(UrgencyLevel.SOON_AMBER, status2Days.urgencyLevel)

        val in5Days = makeTask(dueDate = today.plusDays(5), dueTimeMinutes = null)
        val status5Days = TaskStatusCalculator.evaluate(in5Days, now)
        assertEquals(TaskGroup.UPCOMING, status5Days.group)
        assertEquals("剩 5 天", status5Days.countdownText)
        assertEquals(UrgencyLevel.NORMAL_NEUTRAL, status5Days.urgencyLevel)
    }

    @Test
    fun timedTask_countdownOverdueAndUrgencyLevels() {
        val taskAt1800 = makeTask(dueDate = today, dueTimeMinutes = 18 * 60)

        // 45 minutes left
        val at1715 = LocalDateTime.of(2026, 11, 25, 17, 15)
        val status45m = TaskStatusCalculator.evaluate(taskAt1800, at1715)
        assertEquals("剩 45 分钟", status45m.countdownText)
        assertEquals(TaskGroup.TODAY, status45m.group)
        assertEquals(UrgencyLevel.URGENT_ORANGE, status45m.urgencyLevel)

        // 3 hours left
        val at1500 = LocalDateTime.of(2026, 11, 25, 15, 0)
        val status3h = TaskStatusCalculator.evaluate(taskAt1800, at1500)
        assertEquals("剩 3 小时", status3h.countdownText)
        assertEquals(UrgencyLevel.URGENT_ORANGE, status3h.urgencyLevel)

        // Exactly at 18:00 -> not overdue yet
        val exact1800 = LocalDateTime.of(2026, 11, 25, 18, 0)
        assertFalse(TaskStatusCalculator.isOverdue(taskAt1800, exact1800))

        // 2 hours overdue -> 20:00
        val at2000 = LocalDateTime.of(2026, 11, 25, 20, 0)
        assertTrue(TaskStatusCalculator.isOverdue(taskAt1800, at2000))
        val statusOverdue = TaskStatusCalculator.evaluate(taskAt1800, at2000)
        assertEquals(TaskGroup.OVERDUE, statusOverdue.group)
        assertEquals("已逾期 2 小时", statusOverdue.countdownText)
        assertEquals(UrgencyLevel.OVERDUE_RED, statusOverdue.urgencyLevel)

        // 2 days remaining -> SOON_AMBER
        val future2d = makeTask(dueDate = today.plusDays(2), dueTimeMinutes = 18 * 60)
        val status2d = TaskStatusCalculator.evaluate(future2d, LocalDateTime.of(2026, 11, 25, 15, 0))
        assertEquals("剩 2 天", status2d.countdownText)
        assertEquals(TaskGroup.UPCOMING, status2d.group)
        assertEquals(UrgencyLevel.SOON_AMBER, status2d.urgencyLevel)
    }

    @Test
    fun filterAndSortTodayOngoingTasks_filtersTodayIncompleteAndSortsTimedBeforeAllDay() {
        val tag = TagEntity(id = 1L, name = "实验报告", colorHex = 0xFF059669)
        val tasks = listOf(
            TaskWithTag(makeTask(id = 1, title = "全天较晚创建", dueDate = today, dueTimeMinutes = null, createdAtMillis = 500L), tag),
            TaskWithTag(makeTask(id = 2, title = "全天较早创建", dueDate = today, dueTimeMinutes = null, createdAtMillis = 100L), null),
            TaskWithTag(makeTask(id = 3, title = "下午6点", dueDate = today, dueTimeMinutes = 18 * 60), tag),
            TaskWithTag(makeTask(id = 4, title = "上午9点半", dueDate = today, dueTimeMinutes = 9 * 60 + 30), null),
            TaskWithTag(makeTask(id = 5, title = "已完成今天任务", dueDate = today, dueTimeMinutes = 8 * 60, isCompleted = true), null),
            TaskWithTag(makeTask(id = 6, title = "明天任务", dueDate = today.plusDays(1), dueTimeMinutes = 10 * 60), null),
            TaskWithTag(makeTask(id = 7, title = "昨天逾期任务", dueDate = today.minusDays(1), dueTimeMinutes = null), null)
        )

        val ongoing = TaskStatusCalculator.filterAndSortTodayOngoingTasks(tasks, today)
        assertEquals(listOf(4L, 3L, 2L, 1L), ongoing.map { it.task.id })
    }

    @Test
    fun groupAndSortForHome_respectsTagFilterAndGrouping() {
        val tagA = TagEntity(id = 10L, name = "作业", colorHex = 0xFF2563EB)
        val tagB = TagEntity(id = 20L, name = "考试", colorHex = 0xFFDC2626)
        val now = LocalDateTime.of(2026, 11, 25, 14, 0)

        val all = listOf(
            TaskWithTag(makeTask(id = 1, dueDate = today.minusDays(1), tagId = 10L), tagA),
            TaskWithTag(makeTask(id = 2, dueDate = today, dueTimeMinutes = 18 * 60, tagId = 10L), tagA),
            TaskWithTag(makeTask(id = 3, dueDate = today.plusDays(2), tagId = 20L), tagB),
            TaskWithTag(makeTask(id = 4, dueDate = today, isCompleted = true, tagId = 10L, completedAtMillis = 9000L), tagA)
        )

        val unfiltered = TaskStatusCalculator.groupAndSortForHome(all, null, now)
        assertEquals(1, unfiltered.overdue.size)
        assertEquals(1, unfiltered.today.size)
        assertEquals(1, unfiltered.upcoming.size)
        assertEquals(1, unfiltered.completed.size)

        val onlyTagA = TaskStatusCalculator.groupAndSortForHome(all, 10L, now)
        assertEquals(1, onlyTagA.overdue.size)
        assertEquals(1, onlyTagA.today.size)
        assertEquals(0, onlyTagA.upcoming.size)
        assertEquals(1, onlyTagA.completed.size)
    }

    @Test
    fun computeNextUiRefreshDelayMillis_adaptsToTaskUrgency() {
        val now = LocalDateTime.of(2026, 11, 25, 14, 0, 0)

        // All-day task only -> sleeps until midnight (10 hours = 36,000,000ms + 150ms)
        val allDayOnly = TaskStatusCalculator.groupAndSortForHome(
            listOf(TaskWithTag(makeTask(id = 1, dueDate = today, dueTimeMinutes = null), null)),
            null,
            now
        )
        assertEquals(10L * 3600L * 1000L + 150L, TaskStatusCalculator.computeNextUiRefreshDelayMillis(allDayOnly, now))

        // Timed task 3h 30m away -> next hour text change is in 30m + 1s (from "剩 3 小时" to "剩 2 小时" at 2h59m59s)
        val in3h30m = TaskStatusCalculator.groupAndSortForHome(
            listOf(TaskWithTag(makeTask(id = 2, dueDate = today, dueTimeMinutes = 17 * 60 + 30), null)),
            null,
            now
        )
        assertEquals((30L * 60L + 1L) * 1000L + 150L, TaskStatusCalculator.computeNextUiRefreshDelayMillis(in3h30m, now))

        // Timed task 25m 10s away -> next minute text change is in 10s
        val nowWithSec = LocalDateTime.of(2026, 11, 25, 17, 34, 50)
        val in25m10s = TaskStatusCalculator.groupAndSortForHome(
            listOf(TaskWithTag(makeTask(id = 3, dueDate = today, dueTimeMinutes = 18 * 60), null)),
            null,
            nowWithSec
        )
        assertEquals(10L * 1000L + 150L, TaskStatusCalculator.computeNextUiRefreshDelayMillis(in25m10s, nowWithSec))
    }
}
