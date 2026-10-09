package com.halo.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.halo.core.data.repository.FakeRatesRepository
import com.halo.core.data.repository.FakeWalletRepository
import com.halo.core.data.repository.PlatformAppearanceRepository
import com.halo.core.domain.repository.AppearanceRepository
import com.halo.core.domain.repository.RatesRepository
import com.halo.core.domain.repository.WalletRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.Clock
import javax.inject.Qualifier
import javax.inject.Singleton

/** Scope that outlives any screen — for repositories' background work. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

/**
 * Binds domain interfaces to implementations. To connect a real backend, add e.g. `RemoteWalletRepository`
 * and change the binding here (or split by build flavor: `demo` → fakes, `prod` → remote).
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {

    @Binds
    abstract fun bindWalletRepository(impl: FakeWalletRepository): WalletRepository

    @Binds
    abstract fun bindRatesRepository(impl: FakeRatesRepository): RatesRepository

    @Binds
    abstract fun bindAppearanceRepository(impl: PlatformAppearanceRepository): AppearanceRepository

    companion object {
        @Provides
        @Singleton
        @ApplicationScope
        fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        @Provides
        @Singleton
        fun provideClock(): Clock = Clock.systemDefaultZone()

        @Provides
        @Singleton
        fun providePreferences(@ApplicationContext context: Context): DataStore<Preferences> =
            PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("halo_preferences") }
    }
}
