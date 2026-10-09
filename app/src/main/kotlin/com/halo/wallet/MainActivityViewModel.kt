package com.halo.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.halo.core.domain.repository.AppearanceRepository
import com.halo.core.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val appearance: AppearanceRepository,
) : ViewModel() {

    /** Tooling / deep-link entry: `?theme=dark|light|system`. */
    fun applyThemeParam(value: String) {
        val mode = ThemeMode.entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: return
        viewModelScope.launch { appearance.setThemeMode(mode) }
    }
}
