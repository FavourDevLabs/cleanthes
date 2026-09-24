package dev.favourdevlabs.cleanthes.domain.usecase

import dev.favourdevlabs.cleanthes.domain.model.AuditLogItem
import dev.favourdevlabs.cleanthes.domain.model.CitadelItem
import dev.favourdevlabs.cleanthes.domain.model.CitadelProfile
import javax.crypto.Cipher
import javax.crypto.SecretKey
import dev.favourdevlabs.cleanthes.domain.model.CitadelHistoryItem
import dev.favourdevlabs.cleanthes.domain.model.CitadelFields

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
        ENTRY_HISTORY_VIEWED,
        ENTRY_RESTORED_FROM_HISTORY,
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
interface RequestReAuth {
    enum class SensitiveAction {
        REVEAL_PASSWORD,
        EXPORT,
        DELETE_ENTRY,
        ROTATE_KEY,
        VIEW_HISTORY,
    }

    sealed interface Challenge {
        data class Biometric(val cipher: Cipher) : Challenge
        data object MasterPassword : Challenge
        data object NotRequired : Challenge
    }

    /**
     * Decides what re-auth challenge, if any, must be satisfied before
     * [action] proceeds. Always returns [Challenge.NotRequired] for a DECOY
     * session — a decoy has nothing real to protect, and gating it would be
     * an inconsistency a coercer could notice. For a REAL session: biometric
     * if enrolled, otherwise a master-password prompt — for every
     * [SensitiveAction], including [SensitiveAction.REVEAL_PASSWORD].
     */
    suspend operator fun invoke(action: SensitiveAction): Challenge
}
interface VerifyMasterPassword {
    /** Verifies [password] against the REAL profile's stored hash only. */
    suspend operator fun invoke(password: String): Boolean
}

interface GetCitadelHistory {
    enum class ChangedField {
        TITLE, USERNAME, PASSWORD, WEBSITE, NOTES, TOTP
    }

    suspend operator fun invoke(entryId: Long, key: SecretKey): List<CitadelHistoryItem>
}

/**
 * Fields that differ going from [older] to [newer]. Used to label a
 * history row with what changed at that point in time, without
 * exposing the actual old/new values in a list view. [older] and
 * [newer] may each be a CitadelHistoryItem or the live CitadelItem —
 * CitadelFields lets either be compared against the other.
 */
fun diffHistoryFields(older: CitadelFields, newer: CitadelFields): Set<GetCitadelHistory.ChangedField> =
    buildSet {
        if (older.title != newer.title) add(GetCitadelHistory.ChangedField.TITLE)
        if (older.username != newer.username) add(GetCitadelHistory.ChangedField.USERNAME)
        if (older.password != newer.password) add(GetCitadelHistory.ChangedField.PASSWORD)
        if (older.website != newer.website) add(GetCitadelHistory.ChangedField.WEBSITE)
        if (older.notes != newer.notes) add(GetCitadelHistory.ChangedField.NOTES)
        if (older.totpSecret != newer.totpSecret) add(GetCitadelHistory.ChangedField.TOTP)
    }
interface RestoreCitadelHistory {
    suspend operator fun invoke(historyId: Long, key: SecretKey): Int
}
