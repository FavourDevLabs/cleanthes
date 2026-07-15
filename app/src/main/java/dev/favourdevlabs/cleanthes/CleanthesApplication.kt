package dev.favourdevlabs.cleanthes

import android.app.Activity
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import dagger.hilt.android.HiltAndroidApp
import dev.favourdevlabs.cleanthes.security.session.SessionManager
import javax.inject.Inject

@HiltAndroidApp
class CleanthesApplication : Application() {
    @Inject lateinit var sessionManager: SessionManager

    private var startedActivityCount = 0

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(AppLifecycleTracker())
        createNotificationChannel()
    }

    private inner class AppLifecycleTracker : ActivityLifecycleCallbacks {
        override fun onActivityStarted(activity: Activity) {
            startedActivityCount++
        }

        override fun onActivityStopped(activity: Activity) {
            startedActivityCount--
            if (startedActivityCount == 0) onAppBackgrounded()
        }

        override fun onActivityCreated(
            activity: Activity,
            savedInstanceState: Bundle?,
        ) = Unit

        override fun onActivityResumed(activity: Activity) = Unit

        override fun onActivityPaused(activity: Activity) = Unit

        override fun onActivitySaveInstanceState(
            activity: Activity,
            outState: Bundle,
        ) = Unit

        override fun onActivityDestroyed(activity: Activity) = Unit
    }

    private fun onAppBackgrounded() {
        sessionManager.clearSession()
        // Clipboard is intentionally left alone here — ClipboardHelper's own
        // 60-second timer governs clearing uniformly, in-app or backgrounded.
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel =
            NotificationChannel(
                "clipboard_security",
                "Clipboard Security",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Notifies you when Cleanthes clears a copied value from the clipboard."
            }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
