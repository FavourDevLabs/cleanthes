package dev.favourdevlabs.cleanthes.security.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManagerImpl
    @Inject
    constructor() : SessionManager {
        companion object {
            private const val SESSION_TIMEOUT_MS = 5 * 60 * 1000L
        }

        @Volatile private var sessionKey: SecretKey? = null

        @Volatile private var sessionStartTime: Long = 0L

        private val _lockState = MutableStateFlow(true)
        override val lockState: StateFlow<Boolean> = _lockState.asStateFlow()

        override fun setSessionKey(key: SecretKey) {
            sessionKey = key
            sessionStartTime = System.currentTimeMillis()
            _lockState.value = false
        }

        override fun getSessionKey(): SecretKey? {
            if (isSessionExpired()) {
                clearSession()
                return null
            }
            return sessionKey
        }

        override fun refreshSession() {
            if (sessionKey != null) sessionStartTime = System.currentTimeMillis()
        }

        override fun clearSession() {
            sessionKey = null
            sessionStartTime = 0L
            _lockState.value = true
        }

        private fun isSessionExpired(): Boolean {
            if (sessionKey == null) return true
            return (System.currentTimeMillis() - sessionStartTime) > SESSION_TIMEOUT_MS
        }
    }
