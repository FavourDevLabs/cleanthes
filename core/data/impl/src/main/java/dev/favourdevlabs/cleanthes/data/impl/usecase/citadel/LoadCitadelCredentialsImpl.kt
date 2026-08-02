package dev.favourdevlabs.cleanthes.data.impl.usecase.citadel

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.favourdevlabs.cleanthes.data.api.usecase.LoadCitadelCredentials
import dev.favourdevlabs.cleanthes.data.impl.db.CitadelFilenameProvider
import dev.favourdevlabs.cleanthes.data.impl.prefs.KEY_AUTH_SALT
import dev.favourdevlabs.cleanthes.data.impl.prefs.KEY_BIOMETRIC_ENABLED
import dev.favourdevlabs.cleanthes.data.impl.prefs.KEY_BIOMETRIC_IV
import dev.favourdevlabs.cleanthes.data.impl.prefs.KEY_ENC_SALT
import dev.favourdevlabs.cleanthes.data.impl.prefs.KEY_MASTER_HASH
import dev.favourdevlabs.cleanthes.data.impl.prefs.KEY_CITADEL_EXISTS
import dev.favourdevlabs.cleanthes.data.impl.prefs.KEY_WRAPPED_CITADEL_KEY_BIOMETRIC
import dev.favourdevlabs.cleanthes.data.impl.prefs.KEY_WRAPPED_CITADEL_KEY_PASSWORD
import dev.favourdevlabs.cleanthes.domain.model.CitadelProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LoadCitadelCredentialsImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val filenameProvider: CitadelFilenameProvider,
) : LoadCitadelCredentials {

    override suspend fun invoke(profile: CitadelProfile): LoadCitadelCredentials.Result =
        withContext(Dispatchers.IO) {
            try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                val prefs = EncryptedSharedPreferences.create(
                    context,
                    filenameProvider.prefsFileName(profile),
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
                )

                LoadCitadelCredentials.Result(
                    citadelExists            = prefs.getBoolean(KEY_CITADEL_EXISTS, false),
                    authSalt                 = prefs.getString(KEY_AUTH_SALT, null),
                    encSalt                  = prefs.getString(KEY_ENC_SALT, null),
                    masterHash               = prefs.getString(KEY_MASTER_HASH, null),
                    wrappedCitadelKeyPassword  = prefs.getString(KEY_WRAPPED_CITADEL_KEY_PASSWORD, null),
                    wrappedCitadelKeyBiometric = prefs.getString(KEY_WRAPPED_CITADEL_KEY_BIOMETRIC, null),
                    biometricIv              = prefs.getString(KEY_BIOMETRIC_IV, null),
                    biometricEnabled         = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false),
                )
            } catch (_: Exception) {
                LoadCitadelCredentials.Result(
                    citadelExists            = false,
                    authSalt                 = null,
                    encSalt                  = null,
                    masterHash               = null,
                    wrappedCitadelKeyPassword  = null,
                    wrappedCitadelKeyBiometric = null,
                    biometricIv              = null,
                    biometricEnabled         = false,
                )
            }
        }
}
