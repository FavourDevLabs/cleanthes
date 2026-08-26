package dev.favourdevlabs.cleanthes.data.api.usecase

interface CheckPasswordBreach {
    data class Result(
        val breached: Boolean,
        val breachCount: Int,
    )

    suspend operator fun invoke(password: String): Result
}
