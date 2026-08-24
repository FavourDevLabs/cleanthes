package dev.favourdevlabs.cleanthes.attestation.impl

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.Certificate

private const val ANDROID_KEYSTORE = "AndroidKeyStore"
private const val ATTESTATION_KEY_ALIAS = "cleanthes_attestation_probe_key"

/**
 * Generates a throwaway EC key inside AndroidKeyStore purely to obtain an
 * attestation certificate chain. Not used for any actual encryption —
 * Cleanthes's real vault key is separate. This key can be deleted after
 * the chain is extracted; it exists only to trigger hardware attestation.
 */
internal class AttestationKeyProvider {

    fun generateAndGetChain(): List<Certificate> {
        val challenge = ByteArray(32).also { SecureRandom().nextBytes(it) }

        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

        // Regenerate fresh each call — a stale chain from a prior app install
        // or OS update shouldn't be silently reused.
        if (keyStore.containsAlias(ATTESTATION_KEY_ALIAS)) {
            keyStore.deleteEntry(ATTESTATION_KEY_ALIAS)
        }

        val spec = KeyGenParameterSpec.Builder(
            ATTESTATION_KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY,
        )
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setAlgorithmParameterSpec(java.security.spec.ECGenParameterSpec("secp256r1"))
            .setAttestationChallenge(challenge)
            .build()

        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEYSTORE)
            .apply { initialize(spec) }
            .generateKeyPair()

        return keyStore.getCertificateChain(ATTESTATION_KEY_ALIAS).toList()
    }

    fun cleanup() {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(ATTESTATION_KEY_ALIAS)) {
            keyStore.deleteEntry(ATTESTATION_KEY_ALIAS)
        }
    }
}
