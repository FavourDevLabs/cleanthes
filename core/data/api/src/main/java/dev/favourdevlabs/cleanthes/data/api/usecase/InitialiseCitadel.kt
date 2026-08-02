package dev.favourdevlabs.cleanthes.data.api.usecase

import dev.favourdevlabs.cleanthes.domain.model.CitadelProfile
import javax.crypto.SecretKey

interface InitialiseCitadel {
    data class Result(
        val citadelKey: SecretKey,
        val encSalt: String,
        val wrappedCitadelKeyPassword: String,
        val authSaltBase64: String,
        val masterHashBase64: String,
    )

    /** profile: which citadel this credential set belongs to (REAL or DECOY). */
    suspend operator fun invoke(masterPassword: String, profile: CitadelProfile): Result
}
