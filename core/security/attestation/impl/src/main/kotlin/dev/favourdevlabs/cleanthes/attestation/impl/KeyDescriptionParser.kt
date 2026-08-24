package dev.favourdevlabs.cleanthes.attestation.impl

import dev.favourdevlabs.cleanthes.attestation.api.SecurityLevel
import java.security.cert.X509Certificate

internal const val ATTESTATION_EXTENSION_OID = "1.3.6.1.4.1.11129.2.1.17"

internal data class ParsedKeyDescription(
    val attestationVersion: Int,
    val attestationSecurityLevel: SecurityLevel,
    val keymasterVersion: Int,
    val keymasterSecurityLevel: SecurityLevel,
)

internal fun parseKeyDescription(cert: X509Certificate): ParsedKeyDescription? {
    val extBytes = cert.getExtensionValue(ATTESTATION_EXTENSION_OID) ?: return null
    // Extension value is itself DER-wrapped in an OCTET STRING; unwrap it first.
    val unwrapped = DerReader(extBytes).let { outer ->
        val end = outer.enterTlv()
        outer.readOctetString(end)
    }
    val r = DerReader(unwrapped)
    val seqEnd = r.enterTlv() // top-level KeyDescription SEQUENCE

    val versionEnd = r.enterTlv()
    val attestationVersion = r.readInteger(versionEnd)

    val secLevelEnd = r.enterTlv()
    val attestationSecurityLevel = securityLevelOf(r.readEnumerated(secLevelEnd))

    val kmVersionEnd = r.enterTlv()
    val keymasterVersion = r.readInteger(kmVersionEnd)

    val kmSecLevelEnd = r.enterTlv()
    val keymasterSecurityLevel = securityLevelOf(r.readEnumerated(kmSecLevelEnd))

    return ParsedKeyDescription(
        attestationVersion = attestationVersion.toInt(),
        attestationSecurityLevel = attestationSecurityLevel,
        keymasterVersion = keymasterVersion.toInt(),
        keymasterSecurityLevel = keymasterSecurityLevel,
    )
}

private fun securityLevelOf(value: Int): SecurityLevel = when (value) {
    0 -> SecurityLevel.SOFTWARE
    1 -> SecurityLevel.TRUSTED_ENVIRONMENT
    2 -> SecurityLevel.STRONG_BOX
    else -> SecurityLevel.SOFTWARE
}
