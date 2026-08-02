package dev.favourdevlabs.cleanthes.data.impl.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.favourdevlabs.cleanthes.data.api.AuditLogRepository
import dev.favourdevlabs.cleanthes.data.api.CitadelRepository
import dev.favourdevlabs.cleanthes.data.api.usecase.EnrolBiometric
import dev.favourdevlabs.cleanthes.data.impl.usecase.citadel.GetActiveCitadelProfileImpl
import dev.favourdevlabs.cleanthes.domain.usecase.GetActiveCitadelProfile
import dev.favourdevlabs.cleanthes.data.impl.usecase.citadel.ActivateCitadelProfileImpl
import dev.favourdevlabs.cleanthes.domain.usecase.ActivateCitadelProfile
import dev.favourdevlabs.cleanthes.data.api.usecase.GetFaviconIcon
import dev.favourdevlabs.cleanthes.data.api.usecase.InitialiseCitadel
import dev.favourdevlabs.cleanthes.data.api.usecase.LoadCitadelCredentials
import dev.favourdevlabs.cleanthes.data.impl.repository.AuditLogRepositoryImpl
import dev.favourdevlabs.cleanthes.data.impl.repository.CitadelRepositoryImpl
import dev.favourdevlabs.cleanthes.data.impl.usecase.DeleteCitadelEntryImpl
import dev.favourdevlabs.cleanthes.data.impl.usecase.ExportCitadelImpl
import dev.favourdevlabs.cleanthes.data.impl.usecase.GetAuditLogImpl
import dev.favourdevlabs.cleanthes.data.impl.usecase.GetFaviconIconImpl
import dev.favourdevlabs.cleanthes.data.impl.usecase.GetCitadelEntriesImpl
import dev.favourdevlabs.cleanthes.data.impl.usecase.GetCitadelEntryImpl
import dev.favourdevlabs.cleanthes.data.impl.usecase.ImportCitadelImpl
import dev.favourdevlabs.cleanthes.data.impl.usecase.RecordAuditEventImpl
import dev.favourdevlabs.cleanthes.data.impl.usecase.SaveCitadelEntryImpl
import dev.favourdevlabs.cleanthes.data.impl.usecase.UnlockCitadelImpl
import dev.favourdevlabs.cleanthes.data.impl.usecase.citadel.EnrolBiometricImpl
import dev.favourdevlabs.cleanthes.data.impl.usecase.citadel.InitialiseCitadelImpl
import dev.favourdevlabs.cleanthes.data.impl.usecase.citadel.LoadCitadelCredentialsImpl
import dev.favourdevlabs.cleanthes.data.impl.usecase.citadel.RotateCitadelKeyImpl
import dev.favourdevlabs.cleanthes.domain.usecase.DeleteCitadelEntry
import dev.favourdevlabs.cleanthes.domain.usecase.ExportCitadel
import dev.favourdevlabs.cleanthes.domain.usecase.GetAuditLog
import dev.favourdevlabs.cleanthes.domain.usecase.GetCitadelEntries
import dev.favourdevlabs.cleanthes.domain.usecase.GetCitadelEntry
import dev.favourdevlabs.cleanthes.domain.usecase.ImportCitadel
import dev.favourdevlabs.cleanthes.domain.usecase.RecordAuditEvent
import dev.favourdevlabs.cleanthes.domain.usecase.RotateCitadelKey
import dev.favourdevlabs.cleanthes.domain.usecase.SaveCitadelEntry
import dev.favourdevlabs.cleanthes.domain.usecase.UnlockCitadel
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds @Singleton
    abstract fun bindCitadelRepository(impl: CitadelRepositoryImpl): CitadelRepository

    @Binds @Singleton
    abstract fun bindGetCitadelEntries(impl: GetCitadelEntriesImpl): GetCitadelEntries

    @Binds @Singleton
    abstract fun bindGetCitadelEntry(impl: GetCitadelEntryImpl): GetCitadelEntry

    @Binds @Singleton
    abstract fun bindSaveCitadelEntry(impl: SaveCitadelEntryImpl): SaveCitadelEntry

    @Binds @Singleton
    abstract fun bindDeleteCitadelEntry(impl: DeleteCitadelEntryImpl): DeleteCitadelEntry

    @Binds @Singleton
    abstract fun bindUnlockCitadel(impl: UnlockCitadelImpl): UnlockCitadel

    @Binds @Singleton
    abstract fun bindInitialiseCitadel(impl: InitialiseCitadelImpl): InitialiseCitadel

    @Binds @Singleton
    abstract fun bindEnrolBiometric(impl: EnrolBiometricImpl): EnrolBiometric

    @Binds @Singleton
    abstract fun bindLoadCitadelCredentials(impl: LoadCitadelCredentialsImpl): LoadCitadelCredentials

    @Binds @Singleton
    abstract fun bindGetFaviconIcon(impl: GetFaviconIconImpl): GetFaviconIcon

    @Binds @Singleton
    abstract fun bindAuditLogRepository(impl: AuditLogRepositoryImpl): AuditLogRepository

    @Binds @Singleton
    abstract fun bindRecordAuditEvent(impl: RecordAuditEventImpl): RecordAuditEvent

    @Binds @Singleton
    abstract fun bindGetAuditLog(impl: GetAuditLogImpl): GetAuditLog

    @Binds @Singleton
    abstract fun bindExportCitadel(impl: ExportCitadelImpl): ExportCitadel

    @Binds @Singleton
    abstract fun bindImportCitadel(impl: ImportCitadelImpl): ImportCitadel

    @Binds @Singleton
    abstract fun bindRotateCitadelKey(impl: RotateCitadelKeyImpl): RotateCitadelKey

    @Binds @Singleton
    abstract fun bindActivateCitadelProfile(impl: ActivateCitadelProfileImpl): ActivateCitadelProfile

    @Binds @Singleton
    abstract fun bindGetActiveCitadelProfile(impl: GetActiveCitadelProfileImpl): GetActiveCitadelProfile
}
