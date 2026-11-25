package com.todayddl.app.domain

enum class ReminderOption(
    val label: String,
    val supportsTimed: Boolean,
    val supportsAllDay: Boolean
) {
    NONE("不提醒", supportsTimed = true, supportsAllDay = true),
    AT_TIME("截止准点", supportsTimed = true, supportsAllDay = false),
    BEFORE_15M("提前 15 分钟", supportsTimed = true, supportsAllDay = false),
    BEFORE_30M("提前 30 分钟", supportsTimed = true, supportsAllDay = false),
    BEFORE_1H("提前 1 小时", supportsTimed = true, supportsAllDay = false),
    BEFORE_3H("提前 3 小时", supportsTimed = true, supportsAllDay = false),
    BEFORE_1D("提前 1 天", supportsTimed = true, supportsAllDay = false),
    SAME_DAY_0800("当天 08:00", supportsTimed = false, supportsAllDay = true),
    SAME_DAY_1200("当天 12:00", supportsTimed = false, supportsAllDay = true),
    PREV_DAY_2000("前一天 20:00", supportsTimed = false, supportsAllDay = true);

    fun isCompatibleWith(hasExactTime: Boolean): Boolean =
        if (hasExactTime) supportsTimed else supportsAllDay

    companion object {
        fun optionsForTimeMode(hasExactTime: Boolean): List<ReminderOption> =
            entries.filter { it.isCompatibleWith(hasExactTime) }

        fun defaultForTimeMode(hasExactTime: Boolean): ReminderOption =
            if (hasExactTime) BEFORE_1H else SAME_DAY_0800

        fun fromNameOrDefault(name: String?, hasExactTime: Boolean): ReminderOption {
            val parsed = entries.firstOrNull { it.name == name }
            return if (parsed != null && parsed.isCompatibleWith(hasExactTime)) {
                parsed
            } else {
                defaultForTimeMode(hasExactTime)
            }
        }

        fun adaptForTimeMode(current: ReminderOption, hasExactTime: Boolean): ReminderOption {
            if (current == NONE) return NONE
            return if (current.isCompatibleWith(hasExactTime)) {
                current
            } else {
                defaultForTimeMode(hasExactTime)
            }
        }
    }
}
