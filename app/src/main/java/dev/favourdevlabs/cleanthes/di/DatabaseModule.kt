package dev.favourdevlabs.cleanthes.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.favourdevlabs.cleanthes.data.db.CleanthesDatabase
import dev.favourdevlabs.cleanthes.data.db.VaultDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CleanthesDatabase =
        CleanthesDatabase.getInstance(context)

    @Provides
    @Singleton
    fun provideVaultDao(database: CleanthesDatabase): VaultDao =
        database.vaultDao()
}

