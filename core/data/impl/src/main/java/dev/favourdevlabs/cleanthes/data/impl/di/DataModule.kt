package dev.favourdevlabs.cleanthes.data.impl.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.favourdevlabs.cleanthes.data.api.AuditLogRepository
import dev.favourdevlabs.cleanthes.data.api.VaultRepository
import dev.favourdevlabs.cleanthes.data.api.usecase.EnrolBiometric
import dev.favourdevlabs.cleanthes.data.api.usecase.GetFaviconIcon
import dev.favourdevlabs.cleanthes.data.api.usecase.InitialiseVault
import dev.favourdevlabs.cleanthes.data.api.usecase.LoadVaultCredentials
import dev.favourdevlabs.cleanthes.data.impl.repository.AuditLogRepositoryImpl
import dev.favourdevlabs.cleanthes.data.impl.repository.VaultRepositoryImpl
import dev.favourdevlabs.cleanthes.data.impl.usecase.DeleteVaultEntryImpl
import dev.favourdevlabs.cleanthes.data.impl.usecase.ExportVaultImpl
import dev.favourdevlabs.cleanthes.data.impl.usecase.GetAuditLogImpl
import dev.favourdevlabs.cleanthes.data.impl.usecase.GetFaviconIconImpl
import dev.favourdevlabs.cleanthes.data.impl.usecase.GetVaultEntriesImpl
import dev.favourdevlabs.cleanthes.data.impl.usecase.GetVaultEntryImpl
import dev.favourdevlabs.cleanthes.data.impl.usecase.ImportVaultImpl
import dev.favourdevlabs.cleanthes.data.impl.usecase.RecordAuditEventImpl
import dev.favourdevlabs.cleanthes.data.impl.usecase.SaveVaultEntryImpl
import dev.favourdevlabs.cleanthes.data.impl.usecase.UnlockVaultImpl
import dev.favourdevlabs.cleanthes.data.impl.usecase.vault.EnrolBiometricImpl
import dev.favourdevlabs.cleanthes.data.impl.usecase.vault.InitialiseVaultImpl
import dev.favourdevlabs.cleanthes.data.impl.usecase.vault.LoadVaultCredentialsImpl
import dev.favourdevlabs.cleanthes.domain.usecase.DeleteVaultEntry
import dev.favourdevlabs.cleanthes.domain.usecase.ExportVault
import dev.favourdevlabs.cleanthes.domain.usecase.GetAuditLog
import dev.favourdevlabs.cleanthes.domain.usecase.GetVaultEntries
import dev.favourdevlabs.cleanthes.domain.usecase.GetVaultEntry
import dev.favourdevlabs.cleanthes.domain.usecase.ImportVault
import dev.favourdevlabs.cleanthes.domain.usecase.RecordAuditEvent
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

    @Binds @Singleton
    abstract fun bindInitialiseVault(impl: InitialiseVaultImpl): InitialiseVault

    @Binds @Singleton
    abstract fun bindEnrolBiometric(impl: EnrolBiometricImpl): EnrolBiometric

    @Binds @Singleton
    abstract fun bindLoadVaultCredentials(impl: LoadVaultCredentialsImpl): LoadVaultCredentials

    @Binds @Singleton
    abstract fun bindGetFaviconIcon(impl: GetFaviconIconImpl): GetFaviconIcon

    @Binds @Singleton
    abstract fun bindAuditLogRepository(impl: AuditLogRepositoryImpl): AuditLogRepository

    @Binds @Singleton
    abstract fun bindRecordAuditEvent(impl: RecordAuditEventImpl): RecordAuditEvent

    @Binds @Singleton
    abstract fun bindGetAuditLog(impl: GetAuditLogImpl): GetAuditLog

    @Binds @Singleton
    abstract fun bindExportVault(impl: ExportVaultImpl): ExportVault

    @Binds @Singleton
    abstract fun bindImportVault(impl: ImportVaultImpl): ImportVault
}
