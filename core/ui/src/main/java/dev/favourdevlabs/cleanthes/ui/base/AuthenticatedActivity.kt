package dev.favourdevlabs.cleanthes.ui.base

import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import dev.favourdevlabs.cleanthes.security.session.LastScreenHolder
import dev.favourdevlabs.cleanthes.security.session.SessionManager
import javax.inject.Inject

@AndroidEntryPoint
abstract class AuthenticatedActivity : SecureActivity() {
    @Inject lateinit var sessionManager: SessionManager

    @Inject lateinit var lastScreenHolder: LastScreenHolder

    override fun onStart() {
        super.onStart()
        if (sessionManager.lockState.value) {
            onBeforeRedirect()
            captureCurrentScreen()
            redirectToLogin()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!sessionManager.lockState.value) {
            sessionManager.refreshSession()
        }
    }

    /**
     * Called once, right before a lock-triggered redirect fires. Subclasses
     * that hold unsaved, sensitive user input (e.g. an in-progress vault
     * entry) can override this to snapshot that state so it survives the
     * Activity's impending finish().
     */
    protected open fun onBeforeRedirect() {}

    private fun captureCurrentScreen() {
        // Home is the default destination anyway — no need to capture it.
        if (this::class.java.simpleName == "HomeActivity") return

        val extras =
            intent.extras?.keySet()?.associateWith { key ->
                @Suppress("DEPRECATION")
                intent.extras?.get(key)
            } ?: emptyMap()

        lastScreenHolder.capture(this::class.java.name, extras)
    }

    protected fun redirectToLogin() {
        val intent =
            packageManager.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            } ?: return
        startActivity(intent)
        finish()
    }
}
