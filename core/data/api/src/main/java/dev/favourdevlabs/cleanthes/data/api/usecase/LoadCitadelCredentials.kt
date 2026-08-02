package dev.favourdevlabs.cleanthes.data.api.usecase

import dev.favourdevlabs.cleanthes.domain.model.CitadelProfile

interface LoadCitadelCredentials {
    data class Result(
        val citadelExists: Boolean,
        val authSalt: String?,
        val encSalt: String?,
        val masterHash: String?,
        val wrappedCitadelKeyPassword: String?,
        val wrappedCitadelKeyBiometric: String?,
        val biometricIv: String?,
        val biometricEnabled: Boolean,
    )
    suspend operator fun invoke(profile: CitadelProfile): Result
}
