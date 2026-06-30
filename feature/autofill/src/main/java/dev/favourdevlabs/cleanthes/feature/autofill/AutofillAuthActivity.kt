package dev.favourdevlabs.cleanthes.feature.autofill

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.service.autofill.FillResponse
import android.view.WindowManager
import android.view.autofill.AutofillId
import android.view.autofill.AutofillManager
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.favourdevlabs.cleanthes.domain.model.VaultItem
import dev.favourdevlabs.cleanthes.data.api.VaultRepository
import dev.favourdevlabs.cleanthes.security.session.SessionManager
import dev.favourdevlabs.cleanthes.ui.theme.CleanthesTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class AutofillAuthActivity : AppCompatActivity() {
    @Inject lateinit var sessionManager: SessionManager

    @Inject lateinit var repository: VaultRepository

    companion object {
        const val EXTRA_PACKAGE_NAME = "pkg"
        const val EXTRA_WEB_DOMAIN = "domain"
        const val EXTRA_USERNAME_ID = "uid"
        const val EXTRA_PASSWORD_ID = "pid"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )
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
                    setResult(RESULT_CANCELED)
                    finish()
                    return
                }
        val usernameId = getParcelableExtraCompat<AutofillId>(EXTRA_USERNAME_ID)
        val passwordId = getParcelableExtraCompat<AutofillId>(EXTRA_PASSWORD_ID)
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
        val webDomain = intent.getStringExtra(EXTRA_WEB_DOMAIN)
        val lookupKey = webDomain ?: packageName

        lifecycleScope.launch {
            try {
                val matches =
                    withContext(Dispatchers.IO) {
                        filter(repository.getAllEntries(secretKey), lookupKey)
                    }
                if (matches.isEmpty()) {
                    setResult(RESULT_CANCELED)
                    finish()
                    return@launch
                }
                val response = FillResponse.Builder()
                for (entry in matches) {
                    response.addDataset(
                        DatasetBuilder.build(
                            this@AutofillAuthActivity,
                            usernameId!!,
                            passwordId!!,
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
            } catch (_: Exception) {
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
        entries: List<VaultItem>,
        key: String?,
    ): List<VaultItem> {
        if (key.isNullOrEmpty()) return emptyList()
        val lower = key.lowercase()
        return entries.filter { e ->
            val website = e.website?.lowercase() ?: ""
            val title = e.title.lowercase()
            website.contains(lower) || lower.contains(website) || title.contains(lower)
        }
    }
}
