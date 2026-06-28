package dev.favourdevlabs.cleanthes.data.impl.usecase.vault

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.favourdevlabs.cleanthes.data.api.usecase.LoadVaultCredentials
import dev.favourdevlabs.cleanthes.data.impl.prefs.KEY_AUTH_SALT
import dev.favourdevlabs.cleanthes.data.impl.prefs.KEY_BIOMETRIC_ENABLED
import dev.favourdevlabs.cleanthes.data.impl.prefs.KEY_BIOMETRIC_IV
import dev.favourdevlabs.cleanthes.data.impl.prefs.KEY_ENC_SALT
import dev.favourdevlabs.cleanthes.data.impl.prefs.KEY_MASTER_HASH
import dev.favourdevlabs.cleanthes.data.impl.prefs.KEY_VAULT_EXISTS
import dev.favourdevlabs.cleanthes.data.impl.prefs.KEY_WRAPPED_VAULT_KEY_BIOMETRIC
import dev.favourdevlabs.cleanthes.data.impl.prefs.KEY_WRAPPED_VAULT_KEY_PASSWORD
import dev.favourdevlabs.cleanthes.data.impl.prefs.PREFS_NAME
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LoadVaultCredentialsImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : LoadVaultCredentials {

    override suspend fun invoke(): LoadVaultCredentials.Result =
        withContext(Dispatchers.IO) {
            try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                val prefs = EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
                )

                LoadVaultCredentials.Result(
                    vaultExists              = prefs.getBoolean(KEY_VAULT_EXISTS, false),
                    authSalt                 = prefs.getString(KEY_AUTH_SALT, null),
                    encSalt                  = prefs.getString(KEY_ENC_SALT, null),
                    masterHash               = prefs.getString(KEY_MASTER_HASH, null),
                    wrappedVaultKeyPassword  = prefs.getString(KEY_WRAPPED_VAULT_KEY_PASSWORD, null),
                    wrappedVaultKeyBiometric = prefs.getString(KEY_WRAPPED_VAULT_KEY_BIOMETRIC, null),
                    biometricIv              = prefs.getString(KEY_BIOMETRIC_IV, null),
                    biometricEnabled         = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false),
                )
            } catch (_: Exception) {
                LoadVaultCredentials.Result(
                    vaultExists              = false,
                    authSalt                 = null,
                    encSalt                  = null,
                    masterHash               = null,
                    wrappedVaultKeyPassword  = null,
                    wrappedVaultKeyBiometric = null,
                    biometricIv              = null,
                    biometricEnabled         = false,
                )
            }
        }
}

