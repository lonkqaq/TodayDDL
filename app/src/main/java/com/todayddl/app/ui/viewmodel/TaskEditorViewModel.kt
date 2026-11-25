package com.todayddl.app.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todayddl.app.data.local.entity.TagEntity
import com.todayddl.app.data.local.entity.TaskEntity
import com.todayddl.app.data.repository.DdlRepository
import com.todayddl.app.domain.ReminderOption
import com.todayddl.app.domain.SemesterDateCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

enum class DateInputMode(val label: String) {
    GREGORIAN("公历日期"),
    SEMESTER_WEEK("学期周数")
}

private data class EditorDraft(
    val isLoaded: Boolean = false,
    val existingTask: TaskEntity? = null,
    val title: String = "",
    val note: String = "",
    val dateInputMode: DateInputMode = DateInputMode.GREGORIAN,
    val selectedDate: LocalDate = LocalDate.now(),
    val hasExactTime: Boolean = false,
    val timeHour: Int = 18,
    val timeMinute: Int = 0,
    val selectedTagId: Long? = null,
    val reminderOption: ReminderOption = ReminderOption.SAME_DAY_0800,
    val titleError: String? = null,
    val isSaving: Boolean = false
)

@Immutable
data class TaskEditorUiState(
    val isEditMode: Boolean = false,
    val isLoaded: Boolean = false,
    val title: String = "",
    val note: String = "",
    val dateInputMode: DateInputMode = DateInputMode.GREGORIAN,
    val selectedDate: LocalDate = LocalDate.now(),
    val semesterStartMonday: LocalDate = SemesterDateCalculator.alignToMonday(LocalDate.now()),
    val hasExactTime: Boolean = false,
    val timeHour: Int = 18,
    val timeMinute: Int = 0,
    val selectedTagId: Long? = null,
    val reminderOption: ReminderOption = ReminderOption.SAME_DAY_0800,
    val availableTags: List<TagEntity> = emptyList(),
    val titleError: String? = null,
    val isSaving: Boolean = false
) {
    val modeAPreview: String
        get() = SemesterDateCalculator.formatModeAPreview(semesterStartMonday, selectedDate)

    val modeBPreview: String
        get() = SemesterDateCalculator.formatModeBPreview(selectedDate)

    val pickerWeek: Int
        get() = SemesterDateCalculator.defaultPickerWeek(semesterStartMonday, selectedDate)

    val pickerDayOfWeek: DayOfWeek
        get() = selectedDate.dayOfWeek

    val formattedTime: String
        get() = if (hasExactTime) {
            "%02d:%02d".format(timeHour, timeMinute)
        } else {
            "全天"
        }

    val availableReminderOptions: List<ReminderOption>
        get() = ReminderOption.optionsForTimeMode(hasExactTime)
}

class TaskEditorViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: DdlRepository
) : ViewModel() {

    private val rawTaskId: Long = savedStateHandle.get<Long>("taskId") ?: -1L
    private val editTaskId: Long? = rawTaskId.takeIf { it > 0L }

    private val draftFlow = MutableStateFlow(EditorDraft())

    val uiState: StateFlow<TaskEditorUiState> = combine(
        draftFlow,
        repository.tagsFlow,
        repository.preferencesFlow
    ) { draft, tags, prefs ->
        val validTagId = if (draft.selectedTagId != null && tags.none { it.id == draft.selectedTagId }) {
            null
        } else {
            draft.selectedTagId
        }
        TaskEditorUiState(
            isEditMode = editTaskId != null,
            isLoaded = draft.isLoaded,
            title = draft.title,
            note = draft.note,
            dateInputMode = draft.dateInputMode,
            selectedDate = draft.selectedDate,
            semesterStartMonday = LocalDate.ofEpochDay(prefs.semesterStartMondayEpochDay),
            hasExactTime = draft.hasExactTime,
            timeHour = draft.timeHour,
            timeMinute = draft.timeMinute,
            selectedTagId = validTagId,
            reminderOption = draft.reminderOption,
            availableTags = tags,
            titleError = draft.titleError,
            isSaving = draft.isSaving
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = TaskEditorUiState(isEditMode = editTaskId != null)
    )

    init {
        loadInitialTask()
    }

    private fun loadInitialTask() {
        viewModelScope.launch {
            if (editTaskId == null) {
                draftFlow.update { it.copy(isLoaded = true) }
                return@launch
            }
            val existing = repository.getTaskById(editTaskId)
            if (existing == null) {
                draftFlow.update { it.copy(isLoaded = true) }
                return@launch
            }
            val exactMinutes = existing.dueTimeMinutes
            val hasExact = exactMinutes != null
            val hour = if (exactMinutes != null) exactMinutes / 60 else 18
            val minute = if (exactMinutes != null) exactMinutes % 60 else 0
            draftFlow.update {
                it.copy(
                    isLoaded = true,
                    existingTask = existing,
                    title = existing.title,
                    note = existing.note,
                    selectedDate = LocalDate.ofEpochDay(existing.dueDateEpochDay),
                    hasExactTime = hasExact,
                    timeHour = hour,
                    timeMinute = minute,
                    selectedTagId = existing.tagId,
                    reminderOption = ReminderOption.fromNameOrDefault(
                        name = existing.reminderOption,
                        hasExactTime = hasExact
                    )
                )
            }
        }
    }

    fun onTitleChange(newTitle: String) {
        draftFlow.update {
            it.copy(
                title = newTitle,
                titleError = if (newTitle.isNotBlank()) null else it.titleError
            )
        }
    }

    fun onNoteChange(newNote: String) {
        draftFlow.update { it.copy(note = newNote) }
    }

    fun onDateInputModeChange(mode: DateInputMode) {
        draftFlow.update { it.copy(dateInputMode = mode) }
    }

    fun onGregorianDateSelected(date: LocalDate) {
        draftFlow.update { it.copy(selectedDate = date) }
    }

    fun onSemesterWeekDaySelected(week: Int, dayOfWeek: DayOfWeek) {
        val monday = uiState.value.semesterStartMonday
        val targetDate = SemesterDateCalculator.weekAndDayToDate(
            semesterStartMonday = monday,
            week = week,
            dayOfWeek = dayOfWeek
        )
        draftFlow.update { it.copy(selectedDate = targetDate) }
    }

    fun onToggleExactTime(enabled: Boolean) {
        draftFlow.update { draft ->
            draft.copy(
                hasExactTime = enabled,
                reminderOption = ReminderOption.adaptForTimeMode(draft.reminderOption, enabled)
            )
        }
    }

    fun onExactTimeSelected(hour: Int, minute: Int) {
        draftFlow.update {
            it.copy(
                hasExactTime = true,
                timeHour = hour.coerceIn(0, 23),
                timeMinute = minute.coerceIn(0, 59),
                reminderOption = ReminderOption.adaptForTimeMode(it.reminderOption, true)
            )
        }
    }

    fun onTagSelected(tagId: Long?) {
        draftFlow.update { it.copy(selectedTagId = tagId) }
    }

    fun onReminderOptionSelected(option: ReminderOption) {
        draftFlow.update { it.copy(reminderOption = option) }
    }

    fun saveTask(onSaved: () -> Unit) {
        val current = draftFlow.value
        val trimmedTitle = current.title.trim()
        if (trimmedTitle.isEmpty()) {
            draftFlow.update { it.copy(titleError = "请输入任务名称") }
            return
        }
        if (current.isSaving) return

        draftFlow.update { it.copy(isSaving = true) }
        val dueMinutes = if (current.hasExactTime) {
            (current.timeHour * 60 + current.timeMinute).coerceIn(0, 1439)
        } else {
            null
        }
        val existing = current.existingTask
        val entity = TaskEntity(
            id = existing?.id ?: 0L,
            title = trimmedTitle,
            note = current.note.trim(),
            dueDateEpochDay = current.selectedDate.toEpochDay(),
            dueTimeMinutes = dueMinutes,
            tagId = current.selectedTagId,
            reminderOption = current.reminderOption.name,
            isCompleted = existing?.isCompleted ?: false,
            createdAtMillis = existing?.createdAtMillis ?: System.currentTimeMillis(),
            completedAtMillis = existing?.completedAtMillis
        )

        viewModelScope.launch {
            repository.upsertTask(entity)
            draftFlow.update { it.copy(isSaving = false) }
            onSaved()
        }
    }

    fun deleteCurrentTask(onDeleted: () -> Unit) {
        val id = editTaskId ?: return
        viewModelScope.launch {
            repository.deleteTask(id)
            onDeleted()
        }
    }
}
