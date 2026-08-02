package dev.favourdevlabs.cleanthes.data.impl.usecase.citadel

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.favourdevlabs.cleanthes.data.api.usecase.EnrolBiometric
import dev.favourdevlabs.cleanthes.data.impl.db.CitadelFilenameProvider
import dev.favourdevlabs.cleanthes.data.impl.prefs.KEY_BIOMETRIC_ENABLED
import dev.favourdevlabs.cleanthes.data.impl.prefs.KEY_BIOMETRIC_IV
import dev.favourdevlabs.cleanthes.data.impl.prefs.KEY_WRAPPED_CITADEL_KEY_BIOMETRIC
import dev.favourdevlabs.cleanthes.domain.model.CitadelProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.inject.Inject

/**
 * Biometric enrollment is REAL-profile-only, deliberately — never
 * threaded with a profile param. A duress scenario is precisely when
 * someone can force a fingerprint/face unlock, so the decoy citadel must
 * never be biometric-reachable.
 */
class EnrolBiometricImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val filenameProvider: CitadelFilenameProvider,
) : EnrolBiometric {

    override suspend fun invoke(citadelKey: SecretKey, unlockedCipher: Cipher) =
        withContext(Dispatchers.IO) {
            val wrappedBytes                = unlockedCipher.doFinal(citadelKey.encoded)
            val wrappedCitadelKeyBiometric  = Base64.encodeToString(wrappedBytes, Base64.NO_WRAP)
            val biometricIv                 = Base64.encodeToString(unlockedCipher.iv, Base64.NO_WRAP)

            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                filenameProvider.prefsFileName(CitadelProfile.REAL),
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            ).edit()
                .putBoolean(KEY_BIOMETRIC_ENABLED, true)
                .putString(KEY_WRAPPED_CITADEL_KEY_BIOMETRIC, wrappedCitadelKeyBiometric)
                .putString(KEY_BIOMETRIC_IV, biometricIv)
                .apply()
        }
}
