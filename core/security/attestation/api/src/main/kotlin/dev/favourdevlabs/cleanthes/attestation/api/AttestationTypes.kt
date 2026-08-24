package dev.favourdevlabs.cleanthes.attestation.api

enum class SecurityLevel { SOFTWARE, TRUSTED_ENVIRONMENT, STRONG_BOX }

enum class VerifiedBootState { VERIFIED, SELF_SIGNED, UNVERIFIED, FAILED, UNKNOWN }

data class AttestationResult(
    val chainValid: Boolean,
    val securityLevel: SecurityLevel,
    val keymasterSecurityLevel: SecurityLevel,
    val verifiedBootState: VerifiedBootState,
    val attestationVersion: Int,
    val failureReason: String? = null,
)

interface KeyAttestationVerifier {
    suspend fun verify(): AttestationResult
}
