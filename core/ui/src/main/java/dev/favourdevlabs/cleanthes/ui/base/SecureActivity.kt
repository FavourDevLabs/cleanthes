package dev.favourdevlabs.cleanthes.ui.base

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity

/**
 * Base Activity that unconditionally prevents screenshots, screen recording,
 * and recent-apps thumbnail capture. Not user-configurable — Cleanthes is
 * zero-knowledge; vault-adjacent screens never render into a compositor
 * buffer the OS can persist or expose.
 *
 * All Activities that touch vault contents, credentials, or the master
 * password must extend this (directly or via AuthenticatedActivity).
 */
abstract class SecureActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        forceSecureMode()
    }

    private fun forceSecureMode() {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setRecentsScreenshotEnabled(false)
        }
    }
}
