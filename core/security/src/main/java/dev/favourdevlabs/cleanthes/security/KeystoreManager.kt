package dev.favourdevlabs.cleanthes.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Manages the hardware-backed (Android Keystore) AES key used to wrap/unwrap
 * the vault key via biometric authentication.
 *
 * This key never leaves the device's secure hardware (TEE/StrongBox) and is
 * configured to require a fresh biometric authentication before each use —
 * making the biometric unlock path genuinely crypto-bound, not just an
 * identity gate.
 */
object KeystoreManager {

    private const val KEYSTORE_PROVIDER     = "AndroidKeyStore"
    private const val BIOMETRIC_KEY_ALIAS   = "cleanthes_biometric_wrap_key"
    private const val TRANSFORMATION        = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS   = 128

    private fun keyStore(): KeyStore =
        KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }

    /**
     * Returns true if the biometric wrapping key already exists in the Keystore.
     */
    fun biometricKeyExists(): Boolean =
        keyStore().containsAlias(BIOMETRIC_KEY_ALIAS)

    /**
     * Generates a new hardware-backed AES-256 key requiring biometric auth
     * for every use. Call once during biometric enrollment (setup).
     *
     * If a key with this alias already exists, it is deleted and replaced —
     * callers should only invoke this when (re-)enrolling biometrics.
     */
    fun generateBiometricKey(): SecretKey {
        val ks = keyStore()
        if (ks.containsAlias(BIOMETRIC_KEY_ALIAS)) {
            ks.deleteEntry(BIOMETRIC_KEY_ALIAS)
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER
        )
        val spec = KeyGenParameterSpec.Builder(
            BIOMETRIC_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    /**
     * Deletes the biometric wrapping key — call when the user disables
     * biometric unlock, so a stale key can't be reused.
     */
    fun deleteBiometricKey() {
        val ks = keyStore()
        if (ks.containsAlias(BIOMETRIC_KEY_ALIAS)) {
            ks.deleteEntry(BIOMETRIC_KEY_ALIAS)
        }
    }

    /**
     * Builds a Cipher in ENCRYPT_MODE using the biometric key.
     * Used once at setup time to wrap the vault key — does NOT require
     * biometric auth at this exact moment in our flow, since setup itself
     * already gated entry via the just-created master password screen.
     */
    @Throws(Exception::class)
    fun getEncryptCipher(): Cipher {
        val key = keyStore().getKey(BIOMETRIC_KEY_ALIAS, null) as SecretKey
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return cipher
    }

    /**
     * Builds a Cipher in DECRYPT_MODE using the biometric key and a
     * previously-stored IV. This Cipher MUST be passed into a
     * BiometricPrompt.CryptoObject — the Keystore will refuse to let it
     * perform doFinal() until a fresh biometric authentication succeeds.
     */
    @Throws(Exception::class)
    fun getDecryptCipher(iv: ByteArray): Cipher {
        val key = keyStore().getKey(BIOMETRIC_KEY_ALIAS, null) as SecretKey
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        return cipher
    }
}
