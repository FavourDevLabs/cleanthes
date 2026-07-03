package dev.favourdevlabs.cleanthes.feature.autofill

import android.app.PendingIntent
import android.app.assist.AssistStructure
import android.content.Intent
import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.Presentations
import android.service.autofill.SaveCallback
import android.service.autofill.SaveInfo
import android.service.autofill.SaveRequest
import android.util.Log
import android.view.autofill.AutofillId
import android.widget.RemoteViews
import dagger.hilt.android.AndroidEntryPoint
import dev.favourdevlabs.cleanthes.data.api.VaultRepository
import dev.favourdevlabs.cleanthes.security.session.SessionManager
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CleanthesAutofillService : AutofillService() {
    @Inject lateinit var sessionManager: SessionManager

    @Inject lateinit var repository: VaultRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onFillRequest(
        request: FillRequest,
        signal: CancellationSignal,
        callback: FillCallback,
    ) {
        val contexts = request.fillContexts
        val structure = contexts[contexts.size - 1].structure
        val parsed = StructureParser.parse(structure)

        if (parsed.usernameId == null || parsed.passwordId == null) {
            callback.onSuccess(null)
            return
        }

        val key = parsed.webDomain ?: parsed.packageName ?: ""

        val authIntent =
            Intent(this, AutofillAuthActivity::class.java)
                .putExtra(AutofillAuthActivity.EXTRA_PACKAGE_NAME, parsed.packageName)
                .putExtra(AutofillAuthActivity.EXTRA_WEB_DOMAIN, parsed.webDomain)
                .putExtra(AutofillAuthActivity.EXTRA_USERNAME_ID, parsed.usernameId)
                .putExtra(AutofillAuthActivity.EXTRA_PASSWORD_ID, parsed.passwordId)

        val pending =
            PendingIntent.getActivity(
                this,
                key.hashCode(),
                authIntent,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val locked =
            RemoteViews(packageName, R.layout.autofill_item).apply {
                setTextViewText(R.id.autofill_label, "Cleanthes \u2014 tap to fill")
            }

        val responseBuilder = FillResponse.Builder()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val presentations =
                Presentations
                    .Builder()
                    .setMenuPresentation(locked)
                    .build()
            responseBuilder.setAuthentication(
                arrayOf(parsed.usernameId, parsed.passwordId),
                pending.intentSender,
                presentations,
            )
        } else {
            @Suppress("DEPRECATION")
            responseBuilder.setAuthentication(
                arrayOf(parsed.usernameId, parsed.passwordId),
                pending.intentSender,
                locked,
            )
        }

        responseBuilder.setSaveInfo(
            SaveInfo
                .Builder(
                    SaveInfo.SAVE_DATA_TYPE_PASSWORD,
                    arrayOf(parsed.usernameId, parsed.passwordId),
                ).build(),
        )

        callback.onSuccess(responseBuilder.build())
    }

    override fun onSaveRequest(
        request: SaveRequest,
        callback: SaveCallback,
    ) {
        val secretKey = sessionManager.getSessionKey()
        if (secretKey == null) {
            callback.onSuccess()
            return
        }

        val contexts = request.fillContexts
        if (contexts.isEmpty()) {
            callback.onSuccess()
            return
        }

        val structure = contexts[contexts.size - 1].structure
        val parsed = StructureParser.parse(structure)
        val usernameId = parsed.usernameId
        val passwordId = parsed.passwordId

        if (usernameId == null || passwordId == null) {
            callback.onSuccess()
            return
        }

        val username = extractValue(structure, usernameId)
        val password = extractValue(structure, passwordId)
        val key = parsed.webDomain ?: parsed.packageName ?: ""

        if (username == null || password == null) {
            callback.onSuccess()
            return
        }

        val handler = CoroutineExceptionHandler { _, exception ->
            Log.w(TAG, "onSaveRequest: failed to persist entry", exception)
            callback.onFailure("Could not save to Cleanthes")
        }

        serviceScope.launch(handler) {
            repository.addEntry(
                title = key,
                userName = username,
                plainPassword = password,
                website = key,
                category = "Autofill",
                notes = null,
                isFavorite = false,
                plainTotpSecret = null,
                totpIssuer = null,
                totpDigits = 6,
                totpPeriod = 30,
                totpAlgorithm = "SHA1",
                key = secretKey,
            )
            callback.onSuccess()
        }
    }

    private fun extractValue(
        structure: AssistStructure,
        target: AutofillId,
    ): String? {
        for (i in 0 until structure.windowNodeCount) {
            val v = findValue(structure.getWindowNodeAt(i).rootViewNode, target)
            if (v != null) return v
        }
        return null
    }

    private fun findValue(
        node: AssistStructure.ViewNode,
        target: AutofillId,
    ): String? {
        if (target == node.autofillId) {
            val v = node.autofillValue
            if (v != null && v.isText) return v.textValue.toString()
        }
        for (i in 0 until node.childCount) {
            val r = findValue(node.getChildAt(i), target)
            if (r != null) return r
        }
        return null
    }

    companion object {
        private const val TAG = "CleanthesAutofillService"
    }
}
