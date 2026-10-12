package com.halo.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.halo.core.domain.repository.AppearanceRepository
import com.halo.core.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val appearance: AppearanceRepository,
) : ViewModel() {

    /**
     * The stored appearance. Before the first read it is System, which is right: the configuration already carries
     * the platform's persisted per-app night mode.
     */
    val themeMode: StateFlow<ThemeMode> = appearance.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.System)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { appearance.setThemeMode(mode) }
    }

    /** Tooling / deep-link entry: `?theme=dark|light|system`. */
    fun applyThemeParam(value: String) {
        val mode = ThemeMode.entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: return
        setThemeMode(mode)
    }
}
