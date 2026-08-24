package dev.favourdevlabs.cleanthes.attestation.impl

import dev.favourdevlabs.cleanthes.attestation.api.SecurityLevel
import dev.favourdevlabs.cleanthes.attestation.api.VerifiedBootState
import java.security.cert.X509Certificate

internal const val ATTESTATION_EXTENSION_OID = "1.3.6.1.4.1.11129.2.1.17"
private const val TAG_ROOT_OF_TRUST = 704

internal data class ParsedKeyDescription(
    val attestationVersion: Int,
    val attestationSecurityLevel: SecurityLevel,
    val keymasterVersion: Int,
    val keymasterSecurityLevel: SecurityLevel,
    val verifiedBootState: VerifiedBootState?,
)

internal fun parseKeyDescription(cert: X509Certificate): ParsedKeyDescription? {
    val extBytes = cert.getExtensionValue(ATTESTATION_EXTENSION_OID) ?: return null
    // Extension value is itself DER-wrapped in an OCTET STRING; unwrap it first.
    val unwrapped = DerReader(extBytes).let { outer ->
        val end = outer.enterTlv()
        outer.readOctetString(end)
    }
    val r = DerReader(unwrapped)
    r.enterTlv() // top-level KeyDescription SEQUENCE — value bounds not needed, fields read sequentially

    val versionEnd = r.enterTlv()
    val attestationVersion = r.readInteger(versionEnd)

    val secLevelEnd = r.enterTlv()
    val attestationSecurityLevel = securityLevelOf(r.readEnumerated(secLevelEnd))

    val kmVersionEnd = r.enterTlv()
    val keymasterVersion = r.readInteger(kmVersionEnd)

    val kmSecLevelEnd = r.enterTlv()
    val keymasterSecurityLevel = securityLevelOf(r.readEnumerated(kmSecLevelEnd))

    val challengeEnd = r.enterTlv()
    r.skip(challengeEnd) // attestationChallenge — not needed yet

    val uniqueIdEnd = r.enterTlv()
    r.skip(uniqueIdEnd) // uniqueId — not needed

    val softwareEnforcedEnd = r.enterTlv()
    r.skip(softwareEnforcedEnd) // rootOfTrust lives in teeEnforced, not here

    val teeEnforcedEnd = r.enterTlv()
    val verifiedBootState = findVerifiedBootState(r, teeEnforcedEnd)
    r.skip(teeEnforcedEnd) // land cleanly at the end regardless of where the walk stopped

    return ParsedKeyDescription(
        attestationVersion = attestationVersion.toInt(),
        attestationSecurityLevel = attestationSecurityLevel,
        keymasterVersion = keymasterVersion.toInt(),
        keymasterSecurityLevel = keymasterSecurityLevel,
        verifiedBootState = verifiedBootState,
    )
}

/** Walks an AuthorizationList's EXPLICIT-tagged fields looking for ROOT_OF_TRUST (704). */
private fun findVerifiedBootState(reader: DerReader, authListEnd: Int): VerifiedBootState? {
    while (reader.hasRemaining(authListEnd)) {
        val (tagNumber, elementEnd) = reader.enterTlvWithTagNumber()
        if (tagNumber == TAG_ROOT_OF_TRUST) {
            reader.enterTlv() // enter the EXPLICIT-wrapped RootOfTrust SEQUENCE
            val keyEnd = reader.enterTlv(); reader.skip(keyEnd)       // verifiedBootKey — skip
            val lockedEnd = reader.enterTlv(); reader.skip(lockedEnd) // deviceLocked — skip for now
            val stateEnd = reader.enterTlv()
            val stateValue = reader.readEnumerated(stateEnd)          // verifiedBootState
            return verifiedBootStateOf(stateValue)
        }
        reader.skip(elementEnd)
    }
    return null
}

private fun securityLevelOf(value: Int): SecurityLevel = when (value) {
    0 -> SecurityLevel.SOFTWARE
    1 -> SecurityLevel.TRUSTED_ENVIRONMENT
    2 -> SecurityLevel.STRONG_BOX
    else -> SecurityLevel.SOFTWARE
}

private fun verifiedBootStateOf(value: Int): VerifiedBootState = when (value) {
    0 -> VerifiedBootState.VERIFIED
    1 -> VerifiedBootState.SELF_SIGNED
    2 -> VerifiedBootState.UNVERIFIED
    3 -> VerifiedBootState.FAILED
    else -> VerifiedBootState.UNKNOWN
}
