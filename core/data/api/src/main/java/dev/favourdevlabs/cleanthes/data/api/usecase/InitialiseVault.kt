package dev.favourdevlabs.cleanthes.data.api.usecase

import javax.crypto.SecretKey

interface InitialiseVault {
    data class Result(
        val vaultKey: SecretKey,
        val encSalt: String,
        val wrappedVaultKeyPassword: String,
        val authSaltBase64: String,
        val masterHashBase64: String,
    )
    suspend operator fun invoke(masterPassword: String): Result
}

