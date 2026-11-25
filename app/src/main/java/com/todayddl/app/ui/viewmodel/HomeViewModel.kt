package com.todayddl.app.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todayddl.app.data.local.entity.TagEntity
import com.todayddl.app.data.local.entity.TaskEntity
import com.todayddl.app.data.repository.DdlRepository
import com.todayddl.app.domain.GroupedTasks
import com.todayddl.app.domain.SemesterDateCalculator
import com.todayddl.app.domain.TaskStatusCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

@Immutable
data class HomeUiState(
    val headerDateText: String = "",
    val semesterStartMonday: LocalDate = SemesterDateCalculator.alignToMonday(LocalDate.now()),
    val tags: List<TagEntity> = emptyList(),
    val selectedTagId: Long? = null,
    val groupedTasks: GroupedTasks = GroupedTasks(),
    val isCompletedExpanded: Boolean = false,
    val referenceYear: Int = LocalDate.now().year
)

class HomeViewModel(
    private val repository: DdlRepository
) : ViewModel() {

    private val selectedTagIdFlow = MutableStateFlow<Long?>(null)
    private val isCompletedExpandedFlow = MutableStateFlow(false)
    private val nowFlow = MutableStateFlow(LocalDateTime.now())

    val uiState: StateFlow<HomeUiState> = combine(
        repository.tasksWithTagFlow,
        repository.tagsFlow,
        repository.preferencesFlow,
        selectedTagIdFlow,
        isCompletedExpandedFlow,
        nowFlow
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val tasks = values[0] as List<com.todayddl.app.data.local.model.TaskWithTag>
        @Suppress("UNCHECKED_CAST")
        val tags = values[1] as List<TagEntity>
        val prefs = values[2] as com.todayddl.app.data.preferences.AppPreferences
        val selectedTagId = values[3] as Long?
        val isCompletedExpanded = values[4] as Boolean
        val now = values[5] as LocalDateTime

        val effectiveSelectedTagId = if (selectedTagId != null && tags.none { it.id == selectedTagId }) {
            null
        } else {
            selectedTagId
        }

        val semesterStartMonday = LocalDate.ofEpochDay(prefs.semesterStartMondayEpochDay)
        val grouped = TaskStatusCalculator.groupAndSortForHome(
            tasks = tasks,
            selectedTagId = effectiveSelectedTagId,
            now = now
        )

        HomeUiState(
            headerDateText = SemesterDateCalculator.formatHomeHeader(
                today = now.toLocalDate(),
                semesterStartMonday = semesterStartMonday
            ),
            semesterStartMonday = semesterStartMonday,
            tags = tags,
            selectedTagId = effectiveSelectedTagId,
            groupedTasks = grouped,
            isCompletedExpanded = isCompletedExpanded,
            referenceYear = now.year
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = HomeUiState()
    )

    fun refreshClock() {
        nowFlow.value = LocalDateTime.now()
    }

    fun refreshOngoingNotification(force: Boolean = false) {
        viewModelScope.launch {
            repository.refreshOngoingAndMidnightOnly(force = force)
        }
    }

    fun selectTag(tagId: Long?) {
        selectedTagIdFlow.value = tagId
    }

    fun toggleCompletedSection() {
        isCompletedExpandedFlow.value = !isCompletedExpandedFlow.value
    }

    fun toggleTaskCompletion(task: TaskEntity) {
        viewModelScope.launch {
            repository.toggleTaskCompletion(task)
        }
    }
}
