package dev.favourdevlabs.cleanthes.domain.model

/**
 * Pure domain model — no Room annotations, no Android imports.
 * This is the entity the rest of the app reasons about.
 * CitadelEntry in :core:data:impl maps to/from this.
 */
data class CitadelItem(
    val id: Long = 0,
    override val title: String = "",
    override val username: String = "",
    override val password: String = "",          // plaintext after decryption
    override val website: String? = null,
    val category: String = "General",
    override val notes: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val isFavorite: Boolean = false,
    override val totpSecret: String? = null,     // plaintext after decryption; null = no TOTP
    val totpIssuer: String? = null,
    val totpDigits: Int = 6,
    val totpPeriod: Int = 30,
    val totpAlgorithm: String = "SHA1",
) : CitadelFields {
    fun hasTOTP(): Boolean = !totpSecret.isNullOrEmpty()
}

