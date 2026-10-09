package com.halo.core.domain.repository

import com.halo.core.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/** Persisted appearance preferences. */
interface AppearanceRepository {
    val themeMode: Flow<ThemeMode>

    suspend fun setThemeMode(mode: ThemeMode)
}
