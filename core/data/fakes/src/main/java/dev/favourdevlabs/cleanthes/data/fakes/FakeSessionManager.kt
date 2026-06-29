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

    override fun setSessionKey(key: SecretKey) {
        this.key = key
        _lockState.value = false
    }

    override fun getSessionKey(): SecretKey? = key

    override fun clearSession() {
        key = null
        _lockState.value = true
    }

    override fun refreshSession() = Unit

    override fun getLastActiveTimestamp(): Long = 0L

    // Test helpers
    fun setKey(key: SecretKey) = setSessionKey(key)
    fun lock() = clearSession()
}
