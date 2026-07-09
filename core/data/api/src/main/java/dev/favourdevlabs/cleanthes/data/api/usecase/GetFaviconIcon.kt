package dev.favourdevlabs.cleanthes.data.api.usecase

interface GetFaviconIcon {
    // ByteArray in a data class means equals()/hashCode() compare reference identity,
    // not content — acceptable here since this Result is a one-way carrier from the
    // use case to the UI layer and is never compared for equality.
    @Suppress("ArrayInDataClass")
    data class Result(
        val found: Boolean,
        val bytes: ByteArray?,
        val contentType: String?,
    )

    suspend operator fun invoke(website: String): Result
}
