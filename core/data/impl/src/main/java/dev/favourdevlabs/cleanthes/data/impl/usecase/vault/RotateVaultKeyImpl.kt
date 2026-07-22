package dev.favourdevlabs.cleanthes.data.impl.usecase.vault

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.favourdevlabs.cleanthes.data.api.VaultRepository
import dev.favourdevlabs.cleanthes.data.api.usecase.LoadVaultCredentials
import dev.favourdevlabs.cleanthes.data.impl.db.VaultDatabaseSwitchboard
import dev.favourdevlabs.cleanthes.data.impl.prefs.KEY_ENC_SALT
import dev.favourdevlabs.cleanthes.data.impl.prefs.KEY_WRAPPED_VAULT_KEY_PASSWORD
import dev.favourdevlabs.cleanthes.data.impl.prefs.prefsName
import dev.favourdevlabs.cleanthes.domain.usecase.RotateVaultKey
import dev.favourdevlabs.cleanthes.security.KeyDerivation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.crypto.SecretKey
import javax.inject.Inject

class RotateVaultKeyImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val loadVaultCredentials: LoadVaultCredentials,
    private val vaultRepository: VaultRepository,
    private val switchboard: VaultDatabaseSwitchboard,
) : RotateVaultKey {

    override suspend fun invoke(
        masterPassword: String,
        currentVaultKey: SecretKey,
    ): RotateVaultKey.Result = withContext(Dispatchers.IO) {
        val profile = switchboard.currentProfile()
            ?: throw IllegalStateException("No vault profile active — cannot rotate key while locked")

        val credentials = loadVaultCredentials(profile)
        val authSalt = credentials.authSalt ?: throw IllegalStateException("Vault data missing")
        val masterHash = credentials.masterHash ?: throw IllegalStateException("Vault data missing")

        val verified = KeyDerivation.verifyMasterPassword(masterPassword.toCharArray(), authSalt, masterHash)
        if (!verified) throw SecurityException("Incorrect master password")

        val newVaultKey = KeyDerivation.generateVaultKey()

        // Re-encrypt every entry as a single atomic transaction before
        // committing the new key anywhere — if this throws, nothing below
        // runs and the old key/prefs remain the source of truth.
        vaultRepository.reencryptAllEntries(currentVaultKey, newVaultKey)

        val newEncSaltBytes = KeyDerivation.generateSalt()
        val newEncSalt = Base64.encodeToString(newEncSaltBytes, Base64.NO_WRAP)
        val pwdDerivedKey = KeyDerivation.deriveKey(masterPassword.toCharArray(), newEncSaltBytes)
        val newWrappedVaultKeyPassword = KeyDerivation.wrapKey(newVaultKey, pwdDerivedKey)

        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            prefsName(profile),
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        ).edit()
            .putString(KEY_ENC_SALT, newEncSalt)
            .putString(KEY_WRAPPED_VAULT_KEY_PASSWORD, newWrappedVaultKeyPassword)
            .apply()

        RotateVaultKey.Result(
            newVaultKey = newVaultKey,
            biometricWasEnabled = credentials.biometricEnabled,
        )
    }
}
