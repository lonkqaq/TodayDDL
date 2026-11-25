package com.todayddl.app.domain

import com.todayddl.app.data.local.entity.TaskEntity
import com.todayddl.app.data.local.model.TaskWithTag
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

enum class UrgencyLevel {
    OVERDUE_RED,
    URGENT_ORANGE,
    SOON_AMBER,
    NORMAL_NEUTRAL,
    COMPLETED_MUTED
}

enum class TaskGroup {
    OVERDUE,
    TODAY,
    UPCOMING,
    COMPLETED
}

data class TaskStatusInfo(
    val isOverdue: Boolean,
    val group: TaskGroup,
    val countdownText: String,
    val urgencyLevel: UrgencyLevel
)

data class HomeTaskItem(
    val taskWithTag: TaskWithTag,
    val status: TaskStatusInfo
)

data class GroupedTasks(
    val overdue: List<HomeTaskItem> = emptyList(),
    val today: List<HomeTaskItem> = emptyList(),
    val upcoming: List<HomeTaskItem> = emptyList(),
    val completed: List<HomeTaskItem> = emptyList()
) {
    val isAllEmpty: Boolean
        get() = overdue.isEmpty() && today.isEmpty() && upcoming.isEmpty() && completed.isEmpty()
}

object TaskStatusCalculator {

    private const val SECONDS_PER_MINUTE = 60L
    private const val SECONDS_PER_HOUR = 3600L
    private const val SECONDS_PER_DAY = 86400L

    fun isOverdue(task: TaskEntity, now: LocalDateTime): Boolean {
        if (task.isCompleted) return false
        val dueDate = LocalDate.ofEpochDay(task.dueDateEpochDay)
        val exactMinutes = task.dueTimeMinutes
        return if (exactMinutes != null) {
            val dueDateTime = dueDate.atTime(exactMinutes / 60, exactMinutes % 60)
            now.isAfter(dueDateTime)
        } else {
            // 全天任务在截止日结束（次日 00:00）后才判定为逾期
            now.toLocalDate().isAfter(dueDate)
        }
    }

    fun evaluate(task: TaskEntity, now: LocalDateTime): TaskStatusInfo {
        if (task.isCompleted) {
            return TaskStatusInfo(
                isOverdue = false,
                group = TaskGroup.COMPLETED,
                countdownText = "已完成",
                urgencyLevel = UrgencyLevel.COMPLETED_MUTED
            )
        }

        val dueDate = LocalDate.ofEpochDay(task.dueDateEpochDay)
        val today = now.toLocalDate()
        val exactMinutes = task.dueTimeMinutes

        if (exactMinutes == null) {
            val daysDiff = ChronoUnit.DAYS.between(today, dueDate)
            return when {
                daysDiff < 0L -> TaskStatusInfo(
                    isOverdue = true,
                    group = TaskGroup.OVERDUE,
                    countdownText = "已逾期 ${-daysDiff} 天",
                    urgencyLevel = UrgencyLevel.OVERDUE_RED
                )
                daysDiff == 0L -> TaskStatusInfo(
                    isOverdue = false,
                    group = TaskGroup.TODAY,
                    countdownText = "今日全天",
                    urgencyLevel = UrgencyLevel.URGENT_ORANGE
                )
                daysDiff in 1L..3L -> TaskStatusInfo(
                    isOverdue = false,
                    group = TaskGroup.UPCOMING,
                    countdownText = "剩 $daysDiff 天",
                    urgencyLevel = UrgencyLevel.SOON_AMBER
                )
                else -> TaskStatusInfo(
                    isOverdue = false,
                    group = TaskGroup.UPCOMING,
                    countdownText = "剩 $daysDiff 天",
                    urgencyLevel = UrgencyLevel.NORMAL_NEUTRAL
                )
            }
        }

        val dueDateTime = dueDate.atTime(exactMinutes / 60, exactMinutes % 60)
        if (now.isAfter(dueDateTime)) {
            val overdueSeconds = Duration.between(dueDateTime, now).seconds.coerceAtLeast(1L)
            val countdownText = when {
                overdueSeconds < SECONDS_PER_HOUR -> {
                    val mins = (overdueSeconds + SECONDS_PER_MINUTE - 1L) / SECONDS_PER_MINUTE
                    "已逾期 $mins 分钟"
                }
                overdueSeconds < SECONDS_PER_DAY -> {
                    val hours = overdueSeconds / SECONDS_PER_HOUR
                    "已逾期 $hours 小时"
                }
                else -> {
                    val days = overdueSeconds / SECONDS_PER_DAY
                    "已逾期 $days 天"
                }
            }
            return TaskStatusInfo(
                isOverdue = true,
                group = TaskGroup.OVERDUE,
                countdownText = countdownText,
                urgencyLevel = UrgencyLevel.OVERDUE_RED
            )
        }

        val remainSeconds = Duration.between(now, dueDateTime).seconds.coerceAtLeast(0L)
        val countdownText = when {
            remainSeconds == 0L -> "即将截止"
            remainSeconds < SECONDS_PER_HOUR -> {
                val mins = (remainSeconds + SECONDS_PER_MINUTE - 1L) / SECONDS_PER_MINUTE
                "剩 $mins 分钟"
            }
            remainSeconds < SECONDS_PER_DAY -> {
                val hours = remainSeconds / SECONDS_PER_HOUR
                "剩 $hours 小时"
            }
            else -> {
                val days = remainSeconds / SECONDS_PER_DAY
                "剩 $days 天"
            }
        }

        val group = if (dueDate == today) TaskGroup.TODAY else TaskGroup.UPCOMING
        val urgency = when {
            dueDate == today || remainSeconds < SECONDS_PER_DAY -> UrgencyLevel.URGENT_ORANGE
            remainSeconds < 4L * SECONDS_PER_DAY -> UrgencyLevel.SOON_AMBER
            else -> UrgencyLevel.NORMAL_NEUTRAL
        }

        return TaskStatusInfo(
            isOverdue = false,
            group = group,
            countdownText = countdownText,
            urgencyLevel = urgency
        )
    }

    fun filterAndSortTodayOngoingTasks(
        tasks: List<TaskWithTag>,
        today: LocalDate
    ): List<TaskWithTag> {
        val todayEpochDay = today.toEpochDay()
        return tasks
            .filter { !it.task.isCompleted && it.task.dueDateEpochDay == todayEpochDay }
            .sortedWith(
                compareBy<TaskWithTag>(
                    { it.task.dueTimeMinutes == null },
                    { it.task.dueTimeMinutes ?: Int.MAX_VALUE },
                    { it.task.createdAtMillis },
                    { it.task.id }
                )
            )
    }

    fun groupAndSortForHome(
        tasks: List<TaskWithTag>,
        selectedTagId: Long?,
        now: LocalDateTime
    ): GroupedTasks {
        val filtered = if (selectedTagId == null) {
            tasks
        } else {
            tasks.filter { it.task.tagId == selectedTagId }
        }

        val activeComparator = compareBy<HomeTaskItem>(
            { it.taskWithTag.task.dueDateEpochDay },
            { it.taskWithTag.task.dueTimeMinutes == null },
            { it.taskWithTag.task.dueTimeMinutes ?: Int.MAX_VALUE },
            { it.taskWithTag.task.createdAtMillis },
            { it.taskWithTag.task.id }
        )

        val overdue = mutableListOf<HomeTaskItem>()
        val today = mutableListOf<HomeTaskItem>()
        val upcoming = mutableListOf<HomeTaskItem>()
        val completed = mutableListOf<HomeTaskItem>()

        for (item in filtered) {
            val status = evaluate(item.task, now)
            val homeItem = HomeTaskItem(taskWithTag = item, status = status)
            when (status.group) {
                TaskGroup.OVERDUE -> overdue.add(homeItem)
                TaskGroup.TODAY -> today.add(homeItem)
                TaskGroup.UPCOMING -> upcoming.add(homeItem)
                TaskGroup.COMPLETED -> completed.add(homeItem)
            }
        }

        return GroupedTasks(
            overdue = overdue.sortedWith(activeComparator),
            today = today.sortedWith(activeComparator),
            upcoming = upcoming.sortedWith(activeComparator),
            completed = completed.sortedByDescending {
                it.taskWithTag.task.completedAtMillis ?: it.taskWithTag.task.createdAtMillis
            }
        )
    }
}
