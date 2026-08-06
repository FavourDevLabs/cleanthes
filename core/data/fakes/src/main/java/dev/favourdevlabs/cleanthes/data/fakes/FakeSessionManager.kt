package dev.favourdevlabs.cleanthes.data.fakes
import dev.favourdevlabs.cleanthes.security.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.crypto.SecretKey

class FakeSessionManager : SessionManager {
    private val _lockState = MutableStateFlow(true)
    override val lockState: StateFlow<Boolean> = _lockState.asStateFlow()
    private var key: SecretKey? = null

    var backgroundedCallCount = 0
        private set
    var foregroundedCallCount = 0
        private set

    override fun setSessionKey(key: SecretKey) {
        this.key = key
        _lockState.value = false
    }

    override suspend fun <T> withSessionKey(block: suspend (SecretKey) -> T): T? {
        val currentKey = key ?: return null
        if (_lockState.value) return null
        return block(currentKey)
    }

    override fun clearSession() {
        key = null
        _lockState.value = true
    }

    override fun refreshSession() = Unit

    override fun notifyAppBackgrounded() {
        backgroundedCallCount++
    }

    override fun notifyAppForegrounded() {
        foregroundedCallCount++
    }

    override fun getLastActiveTimestamp(): Long = 0L

    // Test helpers
    fun setKey(key: SecretKey) = setSessionKey(key)
    fun lock() = clearSession()
}
