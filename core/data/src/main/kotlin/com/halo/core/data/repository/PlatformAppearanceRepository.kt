package com.halo.core.data.repository

import android.app.UiModeManager
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.halo.core.data.di.ApplicationScope
import com.halo.core.domain.repository.AppearanceRepository
import com.halo.core.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores the choice in DataStore (source of truth) and applies it through the platform's per-app night mode
 * ([UiModeManager.setApplicationNightMode]), so the splash screen, system bars, resources and Compose all
 * switch together — no flash of the wrong theme on launch.
 */
@Singleton
internal class PlatformAppearanceRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: DataStore<Preferences>,
    @ApplicationScope scope: CoroutineScope,
) : AppearanceRepository {

    override val themeMode: Flow<ThemeMode> = preferences.data.map { prefs ->
        prefs[THEME_MODE]?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } } ?: ThemeMode.System
    }

    // Declared after themeMode: initializers run top to bottom.
    init {
        // The platform persists the per-app night mode itself; re-applying keeps it in sync with DataStore.
        scope.launch { apply(themeMode.first()) }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        preferences.edit { it[THEME_MODE] = mode.name }
        apply(mode)
    }

    private fun apply(mode: ThemeMode) {
        val uiModeManager = context.getSystemService(UiModeManager::class.java) ?: return
        uiModeManager.setApplicationNightMode(
            when (mode) {
                ThemeMode.System -> UiModeManager.MODE_NIGHT_AUTO
                ThemeMode.Light -> UiModeManager.MODE_NIGHT_NO
                ThemeMode.Dark -> UiModeManager.MODE_NIGHT_YES
            },
        )
    }

    private companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }
}
