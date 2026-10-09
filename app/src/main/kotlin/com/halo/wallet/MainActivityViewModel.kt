package com.halo.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.halo.core.domain.repository.AppearanceRepository
import com.halo.core.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val appearance: AppearanceRepository,
) : ViewModel() {

    /**
     * The chosen appearance as soon as it is stored: true / false for Dark / Light, null to follow the system
     * (and before the first read, when the platform's persisted per-app night mode is already right). The
     * platform night mode applies the same choice, but its configuration change lands a few hundred ms later.
     */
    val darkOverride: StateFlow<Boolean?> = appearance.themeMode
        .map { mode ->
            when (mode) {
                ThemeMode.Dark -> true
                ThemeMode.Light -> false
                ThemeMode.System -> null
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Tooling / deep-link entry: `?theme=dark|light|system`. */
    fun applyThemeParam(value: String) {
        val mode = ThemeMode.entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: return
        viewModelScope.launch { appearance.setThemeMode(mode) }
    }
}
