package com.todayddl.app.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

sealed interface SemesterPosition {
    val dayOfWeek: DayOfWeek

    data class BeforeSemester(override val dayOfWeek: DayOfWeek) : SemesterPosition
    data class InSemester(val week: Int, override val dayOfWeek: DayOfWeek) : SemesterPosition
}

object SemesterDateCalculator {
    const val MIN_WEEK: Int = 1
    const val MAX_WEEK: Int = 30

    private val isoDateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun alignToMonday(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun weekAndDayToDate(
        semesterStartMonday: LocalDate,
        week: Int,
        dayOfWeek: DayOfWeek
    ): LocalDate {
        val normalizedMonday = alignToMonday(semesterStartMonday)
        val clampedWeek = week.coerceIn(MIN_WEEK, MAX_WEEK)
        return normalizedMonday
            .plusWeeks((clampedWeek - 1).toLong())
            .plusDays((dayOfWeek.value - 1).toLong())
    }

    fun dateToSemesterInfo(
        semesterStartMonday: LocalDate,
        targetDate: LocalDate
    ): SemesterPosition {
        val normalizedMonday = alignToMonday(semesterStartMonday)
        val daysBetween = ChronoUnit.DAYS.between(normalizedMonday, targetDate)
        return if (daysBetween < 0L) {
            SemesterPosition.BeforeSemester(targetDate.dayOfWeek)
        } else {
            val week = (daysBetween / 7L).toInt() + 1
            SemesterPosition.InSemester(week = week, dayOfWeek = targetDate.dayOfWeek)
        }
    }

    fun defaultPickerWeek(
        semesterStartMonday: LocalDate,
        date: LocalDate
    ): Int = when (val pos = dateToSemesterInfo(semesterStartMonday, date)) {
        is SemesterPosition.BeforeSemester -> MIN_WEEK
        is SemesterPosition.InSemester -> pos.week.coerceIn(MIN_WEEK, MAX_WEEK)
    }

    fun dayOfWeekLabel(dayOfWeek: DayOfWeek): String = when (dayOfWeek) {
        DayOfWeek.MONDAY -> "周一"
        DayOfWeek.TUESDAY -> "周二"
        DayOfWeek.WEDNESDAY -> "周三"
        DayOfWeek.THURSDAY -> "周四"
        DayOfWeek.FRIDAY -> "周五"
        DayOfWeek.SATURDAY -> "周六"
        DayOfWeek.SUNDAY -> "周日"
    }

    fun formatModeAPreview(
        semesterStartMonday: LocalDate,
        selectedDate: LocalDate
    ): String = when (val pos = dateToSemesterInfo(semesterStartMonday, selectedDate)) {
        is SemesterPosition.BeforeSemester -> "对应：开学前"
        is SemesterPosition.InSemester -> "对应：第 ${pos.week} 周 ${dayOfWeekLabel(pos.dayOfWeek)}"
    }

    fun formatModeBPreview(targetDate: LocalDate): String =
        "对应：${targetDate.format(isoDateFormatter)}"

    fun formatHomeHeader(
        today: LocalDate,
        semesterStartMonday: LocalDate
    ): String {
        val dateText = "${today.monthValue}月${today.dayOfMonth}日"
        val semesterText = when (val pos = dateToSemesterInfo(semesterStartMonday, today)) {
            is SemesterPosition.BeforeSemester -> "开学前 ${dayOfWeekLabel(pos.dayOfWeek)}"
            is SemesterPosition.InSemester -> "第${pos.week}周 ${dayOfWeekLabel(pos.dayOfWeek)}"
        }
        return "$dateText · $semesterText"
    }

    fun formatTimeMinutes(dueTimeMinutes: Int?): String {
        if (dueTimeMinutes == null) return "全天"
        val clamped = dueTimeMinutes.coerceIn(0, 1439)
        return "%02d:%02d".format(clamped / 60, clamped % 60)
    }

    fun formatTaskDueSchedule(
        dueDate: LocalDate,
        dueTimeMinutes: Int?,
        semesterStartMonday: LocalDate,
        referenceYear: Int = LocalDate.now().year
    ): String {
        val datePart = if (dueDate.year == referenceYear) {
            "%02d-%02d".format(dueDate.monthValue, dueDate.dayOfMonth)
        } else {
            "%04d-%02d-%02d".format(dueDate.year, dueDate.monthValue, dueDate.dayOfMonth)
        }
        val weekPart = when (val pos = dateToSemesterInfo(semesterStartMonday, dueDate)) {
            is SemesterPosition.BeforeSemester -> "(开学前 ${dayOfWeekLabel(pos.dayOfWeek)})"
            is SemesterPosition.InSemester -> "(第${pos.week}周 ${dayOfWeekLabel(pos.dayOfWeek)})"
        }
        val timePart = formatTimeMinutes(dueTimeMinutes)
        return "$datePart $weekPart $timePart"
    }
}
