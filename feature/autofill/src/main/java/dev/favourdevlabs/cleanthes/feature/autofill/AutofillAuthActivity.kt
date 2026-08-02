package dev.favourdevlabs.cleanthes.feature.autofill

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.service.autofill.FillResponse
import android.util.Log
import android.view.autofill.AutofillId
import android.view.autofill.AutofillManager
import androidx.activity.compose.setContent
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.favourdevlabs.cleanthes.domain.model.CitadelItem
import dev.favourdevlabs.cleanthes.data.api.CitadelRepository
import dev.favourdevlabs.cleanthes.security.session.SessionManager
import dev.favourdevlabs.cleanthes.ui.base.SecureActivity
import dev.favourdevlabs.cleanthes.ui.theme.CleanthesTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URI
import javax.inject.Inject

@AndroidEntryPoint
class AutofillAuthActivity : SecureActivity() {
    @Inject lateinit var sessionManager: SessionManager

    @Inject lateinit var repository: CitadelRepository

    companion object {
        private const val TAG = "AutofillAuthActivity"
        const val EXTRA_PACKAGE_NAME = "pkg"
        const val EXTRA_WEB_DOMAIN = "domain"
        const val EXTRA_USERNAME_ID = "uid"
        const val EXTRA_PASSWORD_ID = "pid"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CleanthesTheme {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                )
            }
        }
        if (sessionManager.lockState.value) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        prompt()
    }

    private fun prompt() {
        BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = deliver()

                override fun onAuthenticationFailed() = Unit

                override fun onAuthenticationError(
                    code: Int,
                    msg: CharSequence,
                ) {
                    Log.w(TAG, "biometric auth error: $code $msg")
                    setResult(RESULT_CANCELED)
                    finish()
                }
            },
        ).authenticate(
            BiometricPrompt.PromptInfo
                .Builder()
                .setTitle("Cleanthes")
                .setSubtitle("Authenticate to fill")
                .setNegativeButtonText("Cancel")
                .build(),
        )
    }

    private fun deliver() {
        val secretKey =
            sessionManager.getSessionKey()
                ?: run {
                    Log.w(TAG, "deliver: no session key, session locked or expired")
                    setResult(RESULT_CANCELED)
                    finish()
                    return
                }
        val usernameId = getParcelableExtraCompat<AutofillId>(EXTRA_USERNAME_ID)
        val passwordId = getParcelableExtraCompat<AutofillId>(EXTRA_PASSWORD_ID)
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
        val webDomain = intent.getStringExtra(EXTRA_WEB_DOMAIN)
        val lookupKey = webDomain ?: packageName

        if (usernameId == null || passwordId == null) {
            Log.w(TAG, "deliver: missing usernameId/passwordId in auth intent")
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        lifecycleScope.launch {
            try {
                val matches =
                    if (lookupKey.isNullOrEmpty()) {
                        emptyList()
                    } else {
                        withContext(Dispatchers.IO) {
                            filter(repository.getEntriesByDomainCandidate(lookupKey, secretKey), lookupKey)
                        }
                    }
                if (matches.isEmpty()) {
                    Log.d(TAG, "deliver: no matching entries for $lookupKey")
                    setResult(RESULT_CANCELED)
                    finish()
                    return@launch
                }
                val response = FillResponse.Builder()
                for (entry in matches) {
                    response.addDataset(
                        DatasetBuilder.build(
                            this@AutofillAuthActivity,
                            usernameId,
                            passwordId,
                            entry,
                        ),
                    )
                }
                sessionManager.refreshSession()
                setResult(
                    RESULT_OK,
                    Intent().putExtra(
                        AutofillManager.EXTRA_AUTHENTICATION_RESULT,
                        response.build(),
                    ),
                )
            } catch (e: Exception) {
                Log.w(TAG, "deliver failed", e)
                setResult(RESULT_CANCELED)
            }
            finish()
        }
    }

    private inline fun <reified T> getParcelableExtraCompat(key: String): T? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(key, T::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(key)
        }

    private fun filter(
        entries: List<CitadelItem>,
        key: String?,
    ): List<CitadelItem> {
        if (key.isNullOrEmpty()) return emptyList()
        val targetHost = registrableDomain(key) ?: key.lowercase()
        return entries.filter { e ->
            val entryHost = e.website?.let { registrableDomain(it) }
            val hostMatch = entryHost != null && entryHost == targetHost
            val titleMatch = e.title.lowercase() == targetHost
            hostMatch || titleMatch
        }
    }

    private fun registrableDomain(input: String): String? {
        val candidate = if (input.contains("://")) input else "https://$input"
        val host =
            try {
                URI(candidate).host
            } catch (_: Exception) {
                null
            } ?: return null
        val lower = host.lowercase()
        val parts = lower.split(".")
        return if (parts.size >= 2) parts.takeLast(2).joinToString(".") else lower
    }
}

