package com.halo.feature.showroom.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.halo.core.designsystem.component.HaloToggle
import com.halo.core.designsystem.component.HaloToggleSize
import com.halo.core.designsystem.foundation.LocalHaloThemeReveal
import com.halo.core.designsystem.theme.HaloTheme
import com.halo.core.domain.repository.AppearanceRepository
import com.halo.core.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppearanceViewModel @Inject constructor(
    private val appearance: AppearanceRepository,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> =
        appearance.themeMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.System)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { appearance.setThemeMode(mode) }
    }
}

/**
 * The app's appearance switch (decision D2): a Large [HaloToggle] whose value is the current theme. Flipping it
 * runs the Tide theme transition, growing from the switch itself.
 */
@Composable
internal fun AppearanceSwitch(
    modifier: Modifier = Modifier,
    viewModel: AppearanceViewModel = hiltViewModel(),
) {
    val dark = HaloTheme.colors.isDark
    val motion = HaloTheme.motion
    val reveal = LocalHaloThemeReveal.current
    val scope = rememberCoroutineScope()
    var origin by remember { mutableStateOf(Offset.Zero) }
    HaloToggle(
        checked = dark,
        onCheckedChange = { toDark ->
            val mode = if (toDark) ThemeMode.Dark else ThemeMode.Light
            scope.launch {
                if (reveal != null) reveal.reveal(origin, motion) { viewModel.setThemeMode(mode) }
                else viewModel.setThemeMode(mode)
            }
        },
        size = HaloToggleSize.Large,
        contentDescription = "Dark mode",
        modifier = modifier.onGloballyPositioned { origin = it.boundsInRoot().center },
    )
}
