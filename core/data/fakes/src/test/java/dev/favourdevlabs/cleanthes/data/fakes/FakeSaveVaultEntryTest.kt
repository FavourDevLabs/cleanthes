package dev.favourdevlabs.cleanthes.data.fakes

import dev.favourdevlabs.cleanthes.domain.usecase.SaveVaultEntry
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import javax.crypto.KeyGenerator

class FakeSaveVaultEntryTest {

    private lateinit var fake: FakeSaveVaultEntry
    private val testKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

    @Before
    fun setUp() {
        fake = FakeSaveVaultEntry()
    }

    @Test
    fun `invoke with New params records call and returns id`() = runTest {
        val params = SaveVaultEntry.Params.New(
            title         = "GitHub",
            username      = "pepe",
            plainPassword = "secret123!",
            website       = "github.com",
            category      = "Work",
            notes         = null,
            isFavorite    = false,
            totpSecret    = null,
            totpIssuer    = null,
            totpDigits    = 6,
            totpPeriod    = 30,
            totpAlgorithm = "SHA1",
            key           = testKey,
        )

        val result = fake(params)

        assertEquals(1L, result)
        assertEquals(1, fake.callCount())
        assertEquals(params, fake.lastCall())
    }

    @Test
    fun `invoke throws when shouldThrow is true`() = runTest {
        fake.shouldThrow = true
        val params = SaveVaultEntry.Params.New(
            title = "X", username = "y", plainPassword = "z!1A",
            website = null, category = "General", notes = null,
            isFavorite = false, totpSecret = null, totpIssuer = null,
            totpDigits = 6, totpPeriod = 30, totpAlgorithm = "SHA1",
            key = testKey,
        )

        var threw = false
        try { fake(params) } catch (_: RuntimeException) { threw = true }
        assertTrue(threw)
    }

    @Test
    fun `reset clears recorded calls`() = runTest {
        val params = SaveVaultEntry.Params.New(
            title = "X", username = "y", plainPassword = "z!1A",
            website = null, category = "General", notes = null,
            isFavorite = false, totpSecret = null, totpIssuer = null,
            totpDigits = 6, totpPeriod = 30, totpAlgorithm = "SHA1",
            key = testKey,
        )
        fake(params)
        fake.reset()
        assertEquals(0, fake.callCount())
    }
}
