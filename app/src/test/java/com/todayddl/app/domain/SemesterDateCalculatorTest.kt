package com.todayddl.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class SemesterDateCalculatorTest {

    @Test
    fun alignToMonday_handlesMondayMidweekSundayCrossMonthAndCrossYear() {
        // Already Monday
        assertEquals(
            LocalDate.of(2026, 9, 7),
            SemesterDateCalculator.alignToMonday(LocalDate.of(2026, 9, 7))
        )
        // Wednesday -> Monday of same week
        assertEquals(
            LocalDate.of(2026, 9, 7),
            SemesterDateCalculator.alignToMonday(LocalDate.of(2026, 9, 9))
        )
        // Sunday -> Monday of same week
        assertEquals(
            LocalDate.of(2026, 9, 7),
            SemesterDateCalculator.alignToMonday(LocalDate.of(2026, 9, 13))
        )
        // Cross-month: Oct 1 (Thu) -> Sep 28 (Mon)
        assertEquals(
            LocalDate.of(2026, 9, 28),
            SemesterDateCalculator.alignToMonday(LocalDate.of(2026, 10, 1))
        )
        // Cross-year: Jan 1, 2027 (Fri) -> Dec 28, 2026 (Mon)
        assertEquals(
            LocalDate.of(2026, 12, 28),
            SemesterDateCalculator.alignToMonday(LocalDate.of(2027, 1, 1))
        )
    }

    @Test
    fun bidirectionalConversion_supportsCrossMonthAndCrossYearRoundTrip() {
        // Non-Monday start input gets normalized to Monday 2026-08-31
        val rawStart = LocalDate.of(2026, 9, 2)
        val expectedMonday = LocalDate.of(2026, 8, 31)

        // Week 1 Wednesday -> 2026-09-02 (cross-month within week 1)
        val week1Wed = SemesterDateCalculator.weekAndDayToDate(rawStart, 1, DayOfWeek.WEDNESDAY)
        assertEquals(LocalDate.of(2026, 9, 2), week1Wed)
        assertEquals(
            SemesterPosition.InSemester(1, DayOfWeek.WEDNESDAY),
            SemesterDateCalculator.dateToSemesterInfo(expectedMonday, week1Wed)
        )

        // Week 13 Wednesday -> 2026-11-25
        val week13Wed = SemesterDateCalculator.weekAndDayToDate(expectedMonday, 13, DayOfWeek.WEDNESDAY)
        assertEquals(LocalDate.of(2026, 11, 25), week13Wed)
        assertEquals(
            SemesterPosition.InSemester(13, DayOfWeek.WEDNESDAY),
            SemesterDateCalculator.dateToSemesterInfo(expectedMonday, week13Wed)
        )

        // Cross-year: Week 19 Friday -> 2027-01-08
        val week19Fri = SemesterDateCalculator.weekAndDayToDate(expectedMonday, 19, DayOfWeek.FRIDAY)
        assertEquals(LocalDate.of(2027, 1, 8), week19Fri)
        assertEquals(
            SemesterPosition.InSemester(19, DayOfWeek.FRIDAY),
            SemesterDateCalculator.dateToSemesterInfo(expectedMonday, week19Fri)
        )
    }

    @Test
    fun dateBeforeSemester_reportsBeforeSemester() {
        val semesterMonday = LocalDate.of(2026, 9, 7)
        val dayBefore = LocalDate.of(2026, 9, 6)

        val pos = SemesterDateCalculator.dateToSemesterInfo(semesterMonday, dayBefore)
        assertTrue(pos is SemesterPosition.BeforeSemester)
        assertEquals(DayOfWeek.SUNDAY, pos.dayOfWeek)
        assertEquals("对应：开学前", SemesterDateCalculator.formatModeAPreview(semesterMonday, dayBefore))
    }

    @Test
    fun formattingHelpers_showExpectedStringsAndNeverLeak2359ForAllDay() {
        val semesterMonday = LocalDate.of(2026, 8, 31)
        val date = LocalDate.of(2026, 11, 25)

        assertEquals("11月25日 · 第13周 周三", SemesterDateCalculator.formatHomeHeader(date, semesterMonday))
        assertEquals("对应：第 13 周 周三", SemesterDateCalculator.formatModeAPreview(semesterMonday, date))
        assertEquals("对应：2026-11-25", SemesterDateCalculator.formatModeBPreview(date))

        val allDaySchedule = SemesterDateCalculator.formatTaskDueSchedule(
            dueDate = date,
            dueTimeMinutes = null,
            semesterStartMonday = semesterMonday,
            referenceYear = 2026
        )
        assertEquals("11-25 (第13周 周三) 全天", allDaySchedule)
        assertFalse(allDaySchedule.contains("23:59"))

        val timedSchedule = SemesterDateCalculator.formatTaskDueSchedule(
            dueDate = date,
            dueTimeMinutes = 18 * 60,
            semesterStartMonday = semesterMonday,
            referenceYear = 2026
        )
        assertEquals("11-25 (第13周 周三) 18:00", timedSchedule)
    }
}
