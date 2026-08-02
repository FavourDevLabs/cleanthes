package dev.favourdevlabs.cleanthes.domain.usecase

import dev.favourdevlabs.cleanthes.domain.model.AuditLogItem
import dev.favourdevlabs.cleanthes.domain.model.CitadelItem
import dev.favourdevlabs.cleanthes.domain.model.CitadelProfile
import javax.crypto.SecretKey

interface SaveCitadelEntry {
    sealed interface Params {
        data class New(
            val title: String,
            val username: String,
            val plainPassword: String,
            val website: String?,
            val category: String,
            val notes: String?,
            val isFavorite: Boolean,
            val totpSecret: String?,
            val totpIssuer: String?,
            val totpDigits: Int,
            val totpPeriod: Int,
            val totpAlgorithm: String,
            val key: SecretKey,
        ) : Params

        data class Edit(
            val item: CitadelItem,
            val plainPassword: String,
            val key: SecretKey,
        ) : Params
    }

    suspend operator fun invoke(params: Params): Long
}

interface GetCitadelEntry {
    suspend operator fun invoke(
        id: Long,
        key: SecretKey,
    ): CitadelItem?
}

interface GetCitadelEntries {
    data class Result(
        val entries: List<CitadelItem>,
        val categories: List<String>,
    )

    suspend operator fun invoke(key: SecretKey): Result
}

interface DeleteCitadelEntry {
    suspend operator fun invoke(id: Long): Int
}

interface UnlockCitadel {
    sealed interface Params {
        data class Password(
            val masterPassword: String,
            val encSalt: String,
            val wrappedCitadelKey: String,
        ) : Params

        data class Biometric(
            val citadelKey: SecretKey,
        ) : Params
    }

    suspend operator fun invoke(params: Params)
}


interface ActivateCitadelProfile {
    suspend operator fun invoke(profile: CitadelProfile)
}

interface GetActiveCitadelProfile {
    /** Returns the currently active profile, or null if the session is locked. */
    suspend operator fun invoke(): CitadelProfile?
}

interface RecordAuditEvent {
    enum class EventType {
        UNLOCK_SUCCESS,
        UNLOCK_FAILURE,
        ENTRY_VIEWED,
        ENTRY_CREATED,
        ENTRY_EDITED,
        ENTRY_DELETED,
        EXPORT,
        KEY_ROTATED,
    }

    suspend operator fun invoke(
        eventType: EventType,
        entryId: Long? = null,
        entryTitle: String? = null,
    )
}

interface GetAuditLog {
    suspend operator fun invoke(): List<AuditLogItem>
}

interface ExportCitadel {
    suspend operator fun invoke(
        exportPassword: String,
        key: SecretKey,
    ): String
}

interface ImportCitadel {
    data class Result(
        val imported: Int,
        val skipped: Int,
    )

    suspend operator fun invoke(
        encryptedBlob: String,
        exportPassword: String,
        key: SecretKey,
    ): Result
}

interface RotateCitadelKey {
    data class Result(
        val newCitadelKey: SecretKey,
        val biometricWasEnabled: Boolean,
    )

    suspend operator fun invoke(
        masterPassword: String,
        currentCitadelKey: SecretKey,
    ): Result
}
