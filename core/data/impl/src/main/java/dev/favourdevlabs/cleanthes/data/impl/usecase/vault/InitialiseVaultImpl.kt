package dev.favourdevlabs.cleanthes.data.impl.usecase.vault

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.favourdevlabs.cleanthes.data.api.usecase.InitialiseVault
import dev.favourdevlabs.cleanthes.data.impl.prefs.KEY_AUTH_SALT
import dev.favourdevlabs.cleanthes.data.impl.prefs.KEY_BIOMETRIC_ENABLED
import dev.favourdevlabs.cleanthes.data.impl.prefs.KEY_ENC_SALT
import dev.favourdevlabs.cleanthes.data.impl.prefs.KEY_MASTER_HASH
import dev.favourdevlabs.cleanthes.data.impl.prefs.KEY_VAULT_EXISTS
import dev.favourdevlabs.cleanthes.data.impl.prefs.KEY_WRAPPED_VAULT_KEY_PASSWORD
import dev.favourdevlabs.cleanthes.data.impl.prefs.PREFS_NAME
import dev.favourdevlabs.cleanthes.security.KeyDerivation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class InitialiseVaultImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : InitialiseVault {

    override suspend fun invoke(masterPassword: String): InitialiseVault.Result =
        withContext(Dispatchers.IO) {
            val storedHash    = KeyDerivation.hashPassword(masterPassword.toCharArray())
            val encSaltBytes  = KeyDerivation.generateSalt()
            val encSalt       = Base64.encodeToString(encSaltBytes, Base64.NO_WRAP)
            val vaultKey      = KeyDerivation.generateVaultKey()
            val pwdDerivedKey = KeyDerivation.deriveKey(masterPassword.toCharArray(), encSaltBytes)
            val wrappedVaultKeyPassword = KeyDerivation.wrapKey(vaultKey, pwdDerivedKey)

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
                .putBoolean(KEY_VAULT_EXISTS, true)
                .putString(KEY_AUTH_SALT, storedHash.saltBase64)
                .putString(KEY_ENC_SALT, encSalt)
                .putString(KEY_MASTER_HASH, storedHash.hashBase64)
                .putBoolean(KEY_BIOMETRIC_ENABLED, false)
                .putString(KEY_WRAPPED_VAULT_KEY_PASSWORD, wrappedVaultKeyPassword)
                .apply()

            InitialiseVault.Result(
                vaultKey               = vaultKey,
                encSalt                = encSalt,
                wrappedVaultKeyPassword = wrappedVaultKeyPassword,
                authSaltBase64         = storedHash.saltBase64,
                masterHashBase64       = storedHash.hashBase64,
            )
        }
}

