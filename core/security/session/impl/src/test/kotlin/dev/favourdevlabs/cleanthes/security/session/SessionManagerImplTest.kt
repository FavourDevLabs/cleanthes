package dev.favourdevlabs.cleanthes.security.session

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import kotlinx.coroutines.test.advanceTimeBy
import org.junit.Test
import javax.crypto.KeyGenerator

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SessionManagerImplTest {
    private val testKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

    @Test
    fun `setSessionKey unlocks the session`() =
        runTest {
            val manager = SessionManagerImpl(backgroundScope)
            manager.setSessionKey(testKey)
            assertFalse(manager.lockState.value)
        }

    @Test
    fun `clearSession locks the session`() =
        runTest {
            val manager = SessionManagerImpl(backgroundScope)
            manager.setSessionKey(testKey)
            manager.clearSession()
            assertTrue(manager.lockState.value)
        }

    @Test
    fun `session stays unlocked if foregrounded within background grace period`() =
        runTest {
            val manager = SessionManagerImpl(backgroundScope)
            manager.setSessionKey(testKey)
            manager.notifyAppBackgrounded()
            advanceTimeBy(15_000L)
            manager.notifyAppForegrounded()
            advanceTimeBy(60_000L)
            assertFalse(manager.lockState.value)
        }

    @Test
    fun `session locks after background grace period elapses`() =
        runTest {
            val manager = SessionManagerImpl(backgroundScope)
            manager.setSessionKey(testKey)
            manager.notifyAppBackgrounded()
            advanceTimeBy(30_001L)
            assertTrue(manager.lockState.value)
        }

    @Test
    fun `session locks after foreground inactivity timeout with no refresh`() =
        runTest {
            val manager = SessionManagerImpl(backgroundScope)
            manager.setSessionKey(testKey)
            advanceTimeBy(120_001L)
            assertTrue(manager.lockState.value)
        }

    @Test
    fun `refreshSession resets the inactivity timer`() =
        runTest {
            val manager = SessionManagerImpl(backgroundScope)
            manager.setSessionKey(testKey)
            advanceTimeBy(90_000L)
            manager.refreshSession()
            advanceTimeBy(90_000L)
            assertFalse(manager.lockState.value)
        }
}
