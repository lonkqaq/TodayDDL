package com.todayddl.app.ui.viewmodel

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.todayddl.app.TodayDdlApplication

object AppViewModelProvider {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            HomeViewModel(todayDdlApplication().container.repository)
        }
        initializer {
            TaskEditorViewModel(
                savedStateHandle = createSavedStateHandle(),
                repository = todayDdlApplication().container.repository
            )
        }
        initializer {
            SettingsViewModel(todayDdlApplication().container.repository)
        }
    }
}

private fun CreationExtras.todayDdlApplication(): TodayDdlApplication =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TodayDdlApplication
