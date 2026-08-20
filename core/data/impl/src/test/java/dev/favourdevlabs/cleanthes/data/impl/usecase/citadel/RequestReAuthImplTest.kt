package dev.favourdevlabs.cleanthes.data.impl.usecase.citadel

import dev.favourdevlabs.cleanthes.data.api.usecase.LoadCitadelCredentials
import dev.favourdevlabs.cleanthes.domain.model.CitadelProfile
import dev.favourdevlabs.cleanthes.domain.usecase.GetActiveCitadelProfile
import dev.favourdevlabs.cleanthes.domain.usecase.RequestReAuth
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private class FakeGetActiveCitadelProfile(
    private val profile: CitadelProfile?,
) : GetActiveCitadelProfile {
    override suspend fun invoke(): CitadelProfile? = profile
}

private class FakeLoadCitadelCredentials(
    private val biometricEnabled: Boolean,
) : LoadCitadelCredentials {
    override suspend fun invoke(profile: CitadelProfile): LoadCitadelCredentials.Result =
        LoadCitadelCredentials.Result(
            citadelExists = true,
            authSalt = "salt",
            encSalt = "encSalt",
            masterHash = "hash",
            wrappedCitadelKeyPassword = "wrapped",
            wrappedCitadelKeyBiometric = null,
            biometricIv = null,
            biometricEnabled = biometricEnabled,
        )
}

/**
 * NOTE: the REAL + biometricEnabled branch (which builds a Cipher via
 * KeystoreManager) cannot be covered here — AndroidKeyStore is not present
 * in a plain JVM unit test. That branch needs on-device manual verification
 * or a future androidTest, not this test source set.
 */
class RequestReAuthImplTest {
    @Test
    fun `decoy profile always returns NotRequired regardless of action`() =
        runTest {
            val requestReAuth =
                RequestReAuthImpl(
                    FakeGetActiveCitadelProfile(CitadelProfile.DECOY),
                    FakeLoadCitadelCredentials(biometricEnabled = true),
                )
            RequestReAuth.SensitiveAction.entries.forEach { action ->
                val result = requestReAuth(action)
                assertEquals(RequestReAuth.Challenge.NotRequired, result)
            }
        }

    @Test
    fun `no active session returns NotRequired`() =
        runTest {
            val requestReAuth =
                RequestReAuthImpl(
                    FakeGetActiveCitadelProfile(null),
                    FakeLoadCitadelCredentials(biometricEnabled = true),
                )
            val result = requestReAuth(RequestReAuth.SensitiveAction.EXPORT)
            assertEquals(RequestReAuth.Challenge.NotRequired, result)
        }

    @Test
    fun `real profile no biometric reveal password returns MasterPassword`() =
        runTest {
            val requestReAuth =
                RequestReAuthImpl(
                    FakeGetActiveCitadelProfile(CitadelProfile.REAL),
                    FakeLoadCitadelCredentials(biometricEnabled = false),
                )
            val result = requestReAuth(RequestReAuth.SensitiveAction.REVEAL_PASSWORD)
            assertEquals(RequestReAuth.Challenge.MasterPassword, result)
        }

    @Test
    fun `real profile no biometric export returns MasterPassword`() =
        runTest {
            val requestReAuth =
                RequestReAuthImpl(
                    FakeGetActiveCitadelProfile(CitadelProfile.REAL),
                    FakeLoadCitadelCredentials(biometricEnabled = false),
                )
            val result = requestReAuth(RequestReAuth.SensitiveAction.EXPORT)
            assertEquals(RequestReAuth.Challenge.MasterPassword, result)
        }

    @Test
    fun `real profile no biometric delete entry returns MasterPassword`() =
        runTest {
            val requestReAuth =
                RequestReAuthImpl(
                    FakeGetActiveCitadelProfile(CitadelProfile.REAL),
                    FakeLoadCitadelCredentials(biometricEnabled = false),
                )
            val result = requestReAuth(RequestReAuth.SensitiveAction.DELETE_ENTRY)
            assertEquals(RequestReAuth.Challenge.MasterPassword, result)
        }

    @Test
    fun `real profile no biometric rotate key returns MasterPassword`() =
        runTest {
            val requestReAuth =
                RequestReAuthImpl(
                    FakeGetActiveCitadelProfile(CitadelProfile.REAL),
                    FakeLoadCitadelCredentials(biometricEnabled = false),
                )
            val result = requestReAuth(RequestReAuth.SensitiveAction.ROTATE_KEY)
            assertEquals(RequestReAuth.Challenge.MasterPassword, result)
        }
}
