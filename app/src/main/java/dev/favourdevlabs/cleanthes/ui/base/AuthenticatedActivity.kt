package dev.favourdevlabs.cleanthes.ui.base

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import dev.favourdevlabs.cleanthes.security.session.SessionManager
import dev.favourdevlabs.cleanthes.ui.auth.LoginActivity
import javax.inject.Inject

@AndroidEntryPoint
abstract class AuthenticatedActivity : AppCompatActivity() {
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
        val intent =
            Intent(this, LoginActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
        startActivity(intent)
        finish()
    }
}
