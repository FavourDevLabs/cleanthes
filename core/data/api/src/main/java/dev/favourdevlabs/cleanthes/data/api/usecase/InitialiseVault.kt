package dev.favourdevlabs.cleanthes.data.api.usecase

import dev.favourdevlabs.cleanthes.domain.model.VaultProfile
import javax.crypto.SecretKey

interface InitialiseVault {
    data class Result(
        val vaultKey: SecretKey,
        val encSalt: String,
        val wrappedVaultKeyPassword: String,
        val authSaltBase64: String,
        val masterHashBase64: String,
    )

    /** profile: which vault this credential set belongs to (REAL or DECOY). */
    suspend operator fun invoke(masterPassword: String, profile: VaultProfile): Result
}
