package dev.favourdevlabs.cleanthes.data.impl.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.favourdevlabs.cleanthes.data.api.VaultRepository
import dev.favourdevlabs.cleanthes.data.impl.repository.VaultRepositoryImpl
import dev.favourdevlabs.cleanthes.data.impl.usecase.DeleteVaultEntryImpl
import dev.favourdevlabs.cleanthes.data.impl.usecase.GetVaultEntriesImpl
import dev.favourdevlabs.cleanthes.data.impl.usecase.GetVaultEntryImpl
import dev.favourdevlabs.cleanthes.data.impl.usecase.SaveVaultEntryImpl
import dev.favourdevlabs.cleanthes.data.impl.usecase.UnlockVaultImpl
import dev.favourdevlabs.cleanthes.domain.usecase.DeleteVaultEntry
import dev.favourdevlabs.cleanthes.domain.usecase.GetVaultEntries
import dev.favourdevlabs.cleanthes.domain.usecase.GetVaultEntry
import dev.favourdevlabs.cleanthes.domain.usecase.SaveVaultEntry
import dev.favourdevlabs.cleanthes.domain.usecase.UnlockVault
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds @Singleton
    abstract fun bindVaultRepository(impl: VaultRepositoryImpl): VaultRepository

    @Binds @Singleton
    abstract fun bindGetVaultEntries(impl: GetVaultEntriesImpl): GetVaultEntries

    @Binds @Singleton
    abstract fun bindGetVaultEntry(impl: GetVaultEntryImpl): GetVaultEntry

    @Binds @Singleton
    abstract fun bindSaveVaultEntry(impl: SaveVaultEntryImpl): SaveVaultEntry

    @Binds @Singleton
    abstract fun bindDeleteVaultEntry(impl: DeleteVaultEntryImpl): DeleteVaultEntry

    @Binds @Singleton
    abstract fun bindUnlockVault(impl: UnlockVaultImpl): UnlockVault
}

