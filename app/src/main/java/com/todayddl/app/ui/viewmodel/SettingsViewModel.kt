package com.todayddl.app.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todayddl.app.data.local.entity.TagEntity
import com.todayddl.app.data.repository.DdlRepository
import com.todayddl.app.domain.SemesterDateCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Immutable
data class SettingsUiState(
    val semesterStartMonday: LocalDate = SemesterDateCalculator.alignToMonday(LocalDate.now()),
    val hideNotificationWhenEmpty: Boolean = false,
    val tags: List<TagEntity> = emptyList(),
    val statusMessage: String? = null
)

class SettingsViewModel(
    private val repository: DdlRepository
) : ViewModel() {

    private val statusMessageFlow = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        repository.preferencesFlow,
        repository.tagsFlow,
        statusMessageFlow
    ) { prefs, tags, message ->
        SettingsUiState(
            semesterStartMonday = LocalDate.ofEpochDay(prefs.semesterStartMondayEpochDay),
            hideNotificationWhenEmpty = prefs.hideNotificationWhenEmpty,
            tags = tags,
            statusMessage = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = SettingsUiState()
    )

    fun updateSemesterStartDate(pickedDate: LocalDate) {
        viewModelScope.launch {
            val aligned = repository.updateSemesterStartDate(pickedDate)
            val dateStr = aligned.format(DateTimeFormatter.ISO_LOCAL_DATE)
            statusMessageFlow.value = if (pickedDate != aligned) {
                "已自动对齐为该周周一：$dateStr"
            } else {
                "第 1 周起始日已更新：$dateStr"
            }
        }
    }

    fun setHideNotificationWhenEmpty(hide: Boolean) {
        viewModelScope.launch {
            repository.setHideNotificationWhenEmpty(hide)
        }
    }

    fun addTag(name: String, colorHex: Long) {
        viewModelScope.launch {
            val id = repository.addTag(name, colorHex)
            if (id <= 0L) {
                statusMessageFlow.value = "标签名称不能为空"
            }
        }
    }

    fun updateTag(tag: TagEntity) {
        viewModelScope.launch {
            repository.updateTag(tag)
        }
    }

    fun deleteTag(tag: TagEntity) {
        viewModelScope.launch {
            repository.deleteTag(tag)
        }
    }

    fun refreshOngoingNotificationNow() {
        viewModelScope.launch {
            repository.refreshOngoingAndMidnightOnly(force = true)
            statusMessageFlow.value = if (repository.hasPostNotificationPermission()) {
                "已刷新状态栏常驻通知"
            } else {
                "未授予通知权限，请先开启系统通知权限"
            }
        }
    }

    fun sendTestReminder() {
        val sent = repository.sendTestReminder()
        statusMessageFlow.value = if (sent) {
            "已发送测试提醒通知"
        } else {
            "发送失败，请先在下方授予通知权限"
        }
    }

    fun clearStatusMessage() {
        statusMessageFlow.value = null
    }
}
