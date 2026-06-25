package dev.favourdevlabs.cleanthes.domain.usecase

import dev.favourdevlabs.cleanthes.domain.model.VaultItem
import javax.crypto.SecretKey

interface SaveVaultEntry {
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
            val item: VaultItem,
            val plainPassword: String,
            val key: SecretKey,
        ) : Params
    }

    suspend operator fun invoke(params: Params): Long
}

interface GetVaultEntry {
    suspend operator fun invoke(id: Long, key: SecretKey): VaultItem?
}

interface GetVaultEntries {
    data class Result(
        val entries: List<VaultItem>,
        val categories: List<String>,
    )
    suspend operator fun invoke(key: SecretKey): Result
}

interface DeleteVaultEntry {
    suspend operator fun invoke(id: Long): Int
}

interface UnlockVault {
    sealed interface Params {
        data class Password(
            val masterPassword: String,
            val encSalt: String,
            val wrappedVaultKey: String,
        ) : Params
        data class Biometric(val vaultKey: SecretKey) : Params
    }
    suspend operator fun invoke(params: Params)
}

