package dev.favourdevlabs.cleanthes.security.session

import dev.favourdevlabs.cleanthes.security.session.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManagerImpl
    @Inject
    constructor(
        @ApplicationScope private val scope: CoroutineScope,
    ) : SessionManager {
        companion object {
            private const val BACKGROUND_GRACE_MS = 30 * 1000L
            private const val INACTIVITY_TIMEOUT_MS = 2 * 60 * 1000L
        }

        @Volatile private var sessionKey: SecretKey? = null
        @Volatile private var sessionStartTime: Long = 0L

        private val _lockState = MutableStateFlow(true)
        override val lockState: StateFlow<Boolean> = _lockState.asStateFlow()

        private var backgroundGraceJob: Job? = null
        private var inactivityJob: Job? = null

        override fun setSessionKey(key: SecretKey) {
            sessionKey = key
            sessionStartTime = System.currentTimeMillis()
            _lockState.value = false
            scheduleInactivityTimeout()
        }

        override suspend fun <T> withSessionKey(block: suspend (SecretKey) -> T): T? {
            val key = sessionKey ?: return null
            if (_lockState.value) return null
            return block(key)
        }

        override fun refreshSession() {
            if (sessionKey != null) {
                sessionStartTime = System.currentTimeMillis()
                scheduleInactivityTimeout()
            }
        }

        override fun clearSession() {
            sessionKey = null
            sessionStartTime = 0L
            _lockState.value = true
            backgroundGraceJob?.cancel()
            inactivityJob?.cancel()
        }

        override fun notifyAppBackgrounded() {
            inactivityJob?.cancel()
            backgroundGraceJob?.cancel()
            if (sessionKey == null) return
            backgroundGraceJob =
                scope.launch {
                    delay(BACKGROUND_GRACE_MS)
                    clearSession()
                }
        }

        override fun notifyAppForegrounded() {
            backgroundGraceJob?.cancel()
            backgroundGraceJob = null
            if (sessionKey != null) scheduleInactivityTimeout()
        }

        override fun getLastActiveTimestamp(): Long = sessionStartTime

        private fun scheduleInactivityTimeout() {
            inactivityJob?.cancel()
            inactivityJob =
                scope.launch {
                    delay(INACTIVITY_TIMEOUT_MS)
                    clearSession()
                }
        }
    }
