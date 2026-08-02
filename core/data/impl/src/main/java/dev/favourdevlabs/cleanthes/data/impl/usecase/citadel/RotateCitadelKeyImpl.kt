package dev.favourdevlabs.cleanthes.data.impl.usecase.citadel

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.favourdevlabs.cleanthes.data.api.CitadelRepository
import dev.favourdevlabs.cleanthes.data.api.usecase.LoadCitadelCredentials
import dev.favourdevlabs.cleanthes.data.impl.db.CitadelDatabaseSwitchboard
import dev.favourdevlabs.cleanthes.data.impl.db.CitadelFilenameProvider
import dev.favourdevlabs.cleanthes.data.impl.prefs.KEY_ENC_SALT
import dev.favourdevlabs.cleanthes.data.impl.prefs.KEY_WRAPPED_CITADEL_KEY_PASSWORD
import dev.favourdevlabs.cleanthes.domain.usecase.RotateCitadelKey
import dev.favourdevlabs.cleanthes.security.KeyDerivation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.crypto.SecretKey
import javax.inject.Inject

class RotateCitadelKeyImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val loadCitadelCredentials: LoadCitadelCredentials,
    private val citadelRepository: CitadelRepository,
    private val switchboard: CitadelDatabaseSwitchboard,
    private val filenameProvider: CitadelFilenameProvider,
) : RotateCitadelKey {

    override suspend fun invoke(
        masterPassword: String,
        currentCitadelKey: SecretKey,
    ): RotateCitadelKey.Result = withContext(Dispatchers.IO) {
        val profile = switchboard.currentProfile()
            ?: throw IllegalStateException("No citadel profile active — cannot rotate key while locked")

        val credentials = loadCitadelCredentials(profile)
        val authSalt = credentials.authSalt ?: throw IllegalStateException("Citadel data missing")
        val masterHash = credentials.masterHash ?: throw IllegalStateException("Citadel data missing")

        val verified = KeyDerivation.verifyMasterPassword(masterPassword.toCharArray(), authSalt, masterHash)
        if (!verified) throw SecurityException("Incorrect master password")

        val newCitadelKey = KeyDerivation.generateCitadelKey()

        citadelRepository.reencryptAllEntries(currentCitadelKey, newCitadelKey)

        val newEncSaltBytes = KeyDerivation.generateSalt()
        val newEncSalt = Base64.encodeToString(newEncSaltBytes, Base64.NO_WRAP)
        val pwdDerivedKey = KeyDerivation.deriveKey(masterPassword.toCharArray(), newEncSaltBytes)
        val newWrappedCitadelKeyPassword = KeyDerivation.wrapKey(newCitadelKey, pwdDerivedKey)

        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            filenameProvider.prefsFileName(profile),
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        ).edit()
            .putString(KEY_ENC_SALT, newEncSalt)
            .putString(KEY_WRAPPED_CITADEL_KEY_PASSWORD, newWrappedCitadelKeyPassword)
            .apply()

        RotateCitadelKey.Result(
            newCitadelKey = newCitadelKey,
            biometricWasEnabled = credentials.biometricEnabled,
        )
    }
}
