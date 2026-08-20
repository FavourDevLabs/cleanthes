package dev.favourdevlabs.cleanthes.data.impl.usecase.citadel

import dev.favourdevlabs.cleanthes.data.api.usecase.LoadCitadelCredentials
import dev.favourdevlabs.cleanthes.domain.model.CitadelProfile
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Test

private class FakeCredentialsForVerify(
    private val authSalt: String?,
    private val masterHash: String?,
) : LoadCitadelCredentials {
    override suspend fun invoke(profile: CitadelProfile): LoadCitadelCredentials.Result =
        LoadCitadelCredentials.Result(
            citadelExists = true,
            authSalt = authSalt,
            encSalt = "encSalt",
            masterHash = masterHash,
            wrappedCitadelKeyPassword = "wrapped",
            wrappedCitadelKeyBiometric = null,
            biometricIv = null,
            biometricEnabled = false,
        )
}

/**
 * NOTE: the actual hash-comparison paths (correct/incorrect password
 * against a real stored hash) cannot be covered here — they call
 * KeyDerivation.verifyMasterPassword, which uses android.util.Base64,
 * unavailable in a plain JVM unit test (throws "not mocked"). Robolectric
 * would fix this but requires network access to Maven Central, which is
 * unavailable in this environment. Those two paths need on-device manual
 * verification or a future androidTest, same limitation already accepted
 * for RequestReAuthImpl's biometric branch.
 */
class VerifyMasterPasswordImplTest {
    @Test
    fun `missing salt returns false`() =
        runTest {
            val verify = VerifyMasterPasswordImpl(FakeCredentialsForVerify(authSalt = null, masterHash = "hash"))
            assertFalse(verify("anything"))
        }

    @Test
    fun `missing hash returns false`() =
        runTest {
            val verify = VerifyMasterPasswordImpl(FakeCredentialsForVerify(authSalt = "salt", masterHash = null))
            assertFalse(verify("anything"))
        }
}
