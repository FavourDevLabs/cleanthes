package dev.favourdevlabs.cleanthes.attestation.impl

import java.security.cert.CertPathValidator
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.cert.PKIXParameters
import java.security.cert.TrustAnchor
import java.security.cert.X509Certificate

internal data class ChainValidationResult(
    val valid: Boolean,
    val failureReason: String? = null,
)

/**
 * Validates a device-generated attestation cert chain against the pinned
 * Google hardware attestation roots. Uses PKIX rather than manual issuer/
 * subject walking — standard X.509 path building, no custom parsing needed
 * here (unlike the KeyDescription extension).
 */
internal class ChainValidator(private val trustedRoots: List<X509Certificate>) {

    fun validate(chain: List<Certificate>): ChainValidationResult {
        if (chain.isEmpty()) {
            return ChainValidationResult(valid = false, failureReason = "Empty certificate chain")
        }

        return try {
            val certFactory = CertificateFactory.getInstance("X.509")
            val certPath = certFactory.generateCertPath(chain)

            val anchors = trustedRoots.map { TrustAnchor(it, null) }.toSet()
            val params = PKIXParameters(anchors).apply {
                // Attestation certs don't carry standard revocation info (CRL/OCSP);
                // Google's own verification samples disable this check too.
                isRevocationEnabled = false
            }

            val validator = CertPathValidator.getInstance("PKIX")
            validator.validate(certPath, params)
            ChainValidationResult(valid = true)
        } catch (e: Exception) {
            ChainValidationResult(valid = false, failureReason = e.message ?: e::class.simpleName)
        }
    }
}
