package dev.favourdevlabs.cleanthes.data.api.usecase

interface LoadVaultCredentials {
    data class Result(
        val vaultExists: Boolean,
        val authSalt: String?,
        val encSalt: String?,
        val masterHash: String?,
        val wrappedVaultKeyPassword: String?,
        val wrappedVaultKeyBiometric: String?,
        val biometricIv: String?,
        val biometricEnabled: Boolean,
    )
    suspend operator fun invoke(): Result
}

