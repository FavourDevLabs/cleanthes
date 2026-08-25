package dev.favourdevlabs.cleanthes.attestation.impl

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.favourdevlabs.cleanthes.attestation.api.AttestationResult
import dev.favourdevlabs.cleanthes.attestation.api.KeyAttestationVerifier
import dev.favourdevlabs.cleanthes.attestation.api.SecurityLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.cert.X509Certificate
import javax.inject.Inject

class KeyAttestationVerifierImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : KeyAttestationVerifier {

    override suspend fun verify(): AttestationResult = withContext(Dispatchers.Default) {
        val keyProvider = AttestationKeyProvider()

        val chain = try {
            keyProvider.generateAndGetChain()
        } catch (e: Exception) {
            return@withContext AttestationResult(
                chainValid = false,
                securityLevel = SecurityLevel.SOFTWARE,
                keymasterSecurityLevel = SecurityLevel.SOFTWARE,
                verifiedBootState = dev.favourdevlabs.cleanthes.attestation.api.VerifiedBootState.UNKNOWN,
                attestationVersion = 0,
                failureReason = "Key generation failed: ${e.message}",
            )
        }

        val roots = RootCertLoader(context).loadRoots()
        val chainResult = ChainValidator(roots).validate(chain)

        val leafCert = chain.first() as X509Certificate
        val parsed = parseKeyDescription(leafCert)

        keyProvider.cleanup()

        AttestationResult(
            chainValid = chainResult.valid,
            securityLevel = parsed?.attestationSecurityLevel ?: SecurityLevel.SOFTWARE,
            keymasterSecurityLevel = parsed?.keymasterSecurityLevel ?: SecurityLevel.SOFTWARE,
            verifiedBootState = parsed?.verifiedBootState
                ?: dev.favourdevlabs.cleanthes.attestation.api.VerifiedBootState.UNKNOWN,
            attestationVersion = parsed?.attestationVersion ?: 0,
            failureReason = if (!chainResult.valid) chainResult.failureReason else null,
        )
    }
}
