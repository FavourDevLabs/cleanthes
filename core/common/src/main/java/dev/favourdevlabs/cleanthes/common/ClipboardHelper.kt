package dev.favourdevlabs.cleanthes.common

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object ClipboardHelper {
    private const val CLEAR_DELAY_MS = 60_000L
    private const val CLIP_LABEL_PASSWORD = "Cleanthes Password"
    private const val CLIP_USERNAME = "Cleanthes Username"
    private const val CLIP_LABEL_GENERIC = "Cleanthes"
    private const val CHANNEL_ID = "clipboard_security"
    private const val NOTIFICATION_ID = 1001

    private var pendingClearRunnable: Runnable? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    fun copyPassword(
        context: Context,
        password: String,
    ) = copyToClipboard(context, CLIP_LABEL_PASSWORD, password, isSecret = true)

    fun copyUsername(
        context: Context,
        username: String,
    ) = copyToClipboard(context, CLIP_USERNAME, username, isSecret = true)

    fun copyText(
        context: Context,
        text: String,
    ) = copyToClipboard(context, CLIP_LABEL_GENERIC, text, isSecret = true)

    fun clearClipboard(context: Context) {
        cancelPendingClear()
        overwriteClipboard(context)
    }

    fun hasPendingClear(): Boolean = pendingClearRunnable != null

    private fun copyToClipboard(
        context: Context,
        label: String,
        text: String,
        isSecret: Boolean,
    ) {
        cancelPendingClear()
        val clipboard =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))

        val appContext = context.applicationContext
        pendingClearRunnable =
            Runnable {
                overwriteClipboard(appContext)
                notifyCleared(appContext)
                pendingClearRunnable = null
            }.also { mainHandler.postDelayed(it, CLEAR_DELAY_MS) }
    }

    private fun overwriteClipboard(context: Context) {
        val clipboard =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText(CLIP_LABEL_GENERIC, ""))
    }

    private fun cancelPendingClear() {
        pendingClearRunnable?.let {
            mainHandler.removeCallbacks(it)
            pendingClearRunnable = null
        }
    }

    private fun notifyCleared(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val notification =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_lock)
                .setContentTitle("The clipboard remembers nothing")
                .setContentText("As it should. What you copied has been wiped clean.")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true)
                .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}
