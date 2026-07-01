package dev.favourdevlabs.cleanthes.ui.base

import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import dev.favourdevlabs.cleanthes.security.session.SessionManager
import javax.inject.Inject

@AndroidEntryPoint
abstract class AuthenticatedActivity : SecureActivity() {

    @Inject lateinit var sessionManager: SessionManager

    override fun onStart() {
        super.onStart()
        if (sessionManager.lockState.value) {
            redirectToLogin()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!sessionManager.lockState.value) {
            sessionManager.refreshSession()
        }
    }

    protected fun redirectToLogin() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        } ?: return
        startActivity(intent)
        finish()
    }
}
