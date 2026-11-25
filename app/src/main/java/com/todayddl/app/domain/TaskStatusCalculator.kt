package com.todayddl.app.domain

import androidx.compose.runtime.Immutable
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

@Immutable
data class TaskStatusInfo(
    val isOverdue: Boolean,
    val group: TaskGroup,
    val countdownText: String,
    val urgencyLevel: UrgencyLevel
)

@Immutable
data class HomeTaskItem(
    val taskWithTag: TaskWithTag,
    val status: TaskStatusInfo
)

@Immutable
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

    private val TODAY_ONGOING_COMPARATOR = compareBy<TaskWithTag>(
        { it.task.dueTimeMinutes == null },
        { it.task.dueTimeMinutes ?: Int.MAX_VALUE },
        { it.task.createdAtMillis },
        { it.task.id }
    )

    private val HOME_ACTIVE_COMPARATOR = compareBy<HomeTaskItem>(
        { it.taskWithTag.task.dueDateEpochDay },
        { it.taskWithTag.task.dueTimeMinutes == null },
        { it.taskWithTag.task.dueTimeMinutes ?: Int.MAX_VALUE },
        { it.taskWithTag.task.createdAtMillis },
        { it.taskWithTag.task.id }
    )

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
            .sortedWith(TODAY_ONGOING_COMPARATOR)
    }

    fun sortTodayOngoingTasks(tasks: List<TaskWithTag>): List<TaskWithTag> =
        tasks.sortedWith(TODAY_ONGOING_COMPARATOR)

    fun groupAndSortForHome(
        tasks: List<TaskWithTag>,
        selectedTagId: Long?,
        now: LocalDateTime
    ): GroupedTasks {
        val overdue = mutableListOf<HomeTaskItem>()
        val today = mutableListOf<HomeTaskItem>()
        val upcoming = mutableListOf<HomeTaskItem>()
        val completed = mutableListOf<HomeTaskItem>()

        for (item in tasks) {
            if (selectedTagId != null && item.task.tagId != selectedTagId) continue
            val status = evaluate(item.task, now)
            val homeItem = HomeTaskItem(taskWithTag = item, status = status)
            when (status.group) {
                TaskGroup.OVERDUE -> overdue.add(homeItem)
                TaskGroup.TODAY -> today.add(homeItem)
                TaskGroup.UPCOMING -> upcoming.add(homeItem)
                TaskGroup.COMPLETED -> completed.add(homeItem)
            }
        }

        overdue.sortWith(HOME_ACTIVE_COMPARATOR)
        today.sortWith(HOME_ACTIVE_COMPARATOR)
        upcoming.sortWith(HOME_ACTIVE_COMPARATOR)
        completed.sortByDescending {
            it.taskWithTag.task.completedAtMillis ?: it.taskWithTag.task.createdAtMillis
        }

        return GroupedTasks(
            overdue = overdue,
            today = today,
            upcoming = upcoming,
            completed = completed
        )
    }

    /**
     * Computes the exact delay in milliseconds until any visible countdown text or
     * date header on the Home screen will next change, avoiding unnecessary 1-minute polling.
     */
    fun computeNextUiRefreshDelayMillis(
        groupedTasks: GroupedTasks,
        now: LocalDateTime
    ): Long {
        val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
        var minDelaySeconds = Duration.between(now, nextMidnight).seconds.coerceAtLeast(1L)

        fun inspectList(items: List<HomeTaskItem>) {
            for (item in items) {
                val task = item.taskWithTag.task
                val exactMinutes = task.dueTimeMinutes ?: continue
                val dueDateTime = LocalDate.ofEpochDay(task.dueDateEpochDay)
                    .atTime(exactMinutes / 60, exactMinutes % 60)

                val candidateSeconds = if (now.isAfter(dueDateTime)) {
                    val overdueSeconds = Duration.between(dueDateTime, now).seconds.coerceAtLeast(1L)
                    when {
                        overdueSeconds < SECONDS_PER_HOUR -> {
                            val toNextMinute = SECONDS_PER_MINUTE - ((overdueSeconds - 1L) % SECONDS_PER_MINUTE)
                            minOf(toNextMinute, SECONDS_PER_HOUR - overdueSeconds)
                        }
                        overdueSeconds < SECONDS_PER_DAY -> {
                            SECONDS_PER_HOUR - (overdueSeconds % SECONDS_PER_HOUR)
                        }
                        else -> {
                            SECONDS_PER_DAY - (overdueSeconds % SECONDS_PER_DAY)
                        }
                    }
                } else {
                    val remainSeconds = Duration.between(now, dueDateTime).seconds.coerceAtLeast(0L)
                    when {
                        remainSeconds == 0L -> 1L
                        remainSeconds < SECONDS_PER_HOUR -> {
                            ((remainSeconds - 1L) % SECONDS_PER_MINUTE) + 1L
                        }
                        remainSeconds < SECONDS_PER_DAY -> {
                            (remainSeconds % SECONDS_PER_HOUR) + 1L
                        }
                        else -> {
                            (remainSeconds % SECONDS_PER_DAY) + 1L
                        }
                    }
                }

                if (candidateSeconds < minDelaySeconds) {
                    minDelaySeconds = candidateSeconds.coerceAtLeast(1L)
                }
            }
        }

        inspectList(groupedTasks.overdue)
        inspectList(groupedTasks.today)
        inspectList(groupedTasks.upcoming)

        // Add small 150ms margin to cross the second boundary cleanly.
        return minDelaySeconds * 1_000L + 150L
    }
}
