package dev.favourdevlabs.cleanthes.data.impl.usecase.vault

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.favourdevlabs.cleanthes.data.api.usecase.EnrolBiometric
import dev.favourdevlabs.cleanthes.data.impl.prefs.KEY_BIOMETRIC_ENABLED
import dev.favourdevlabs.cleanthes.data.impl.prefs.KEY_BIOMETRIC_IV
import dev.favourdevlabs.cleanthes.data.impl.prefs.KEY_WRAPPED_VAULT_KEY_BIOMETRIC
import dev.favourdevlabs.cleanthes.data.impl.prefs.PREFS_NAME
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.inject.Inject

class EnrolBiometricImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : EnrolBiometric {

    override suspend fun invoke(vaultKey: SecretKey, unlockedCipher: Cipher) =
        withContext(Dispatchers.IO) {
            val wrappedBytes           = unlockedCipher.doFinal(vaultKey.encoded)
            val wrappedVaultKeyBiometric = Base64.encodeToString(wrappedBytes, Base64.NO_WRAP)
            val biometricIv            = Base64.encodeToString(unlockedCipher.iv, Base64.NO_WRAP)

            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            ).edit()
                .putBoolean(KEY_BIOMETRIC_ENABLED, true)
                .putString(KEY_WRAPPED_VAULT_KEY_BIOMETRIC, wrappedVaultKeyBiometric)
                .putString(KEY_BIOMETRIC_IV, biometricIv)
                .apply()
        }
}

