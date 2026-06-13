package dev.favourdevlabs.cleanthes.autofill

import android.app.PendingIntent
import android.app.assist.AssistStructure
import android.content.Intent
import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.*
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import dagger.hilt.android.AndroidEntryPoint
import dev.favourdevlabs.cleanthes.R
import dev.favourdevlabs.cleanthes.data.repository.VaultRepository
import dev.favourdevlabs.cleanthes.ui.auth.SessionManager
import javax.inject.Inject
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CleanthesAutofillService : AutofillService() {

    @Inject lateinit var sessionManager: SessionManager
    @Inject lateinit var repository: VaultRepository

    override fun onFillRequest(
        request: FillRequest,
        signal: CancellationSignal,
        callback: FillCallback
    ) {
        val contexts  = request.fillContexts
        val structure = contexts[contexts.size - 1].structure
        val parsed    = StructureParser.parse(structure)

        if (parsed.usernameId == null || parsed.passwordId == null) {
            callback.onSuccess(null); return
        }

        val key = parsed.webDomain ?: parsed.packageName ?: ""

        val authIntent = Intent(this, AutofillAuthActivity::class.java)
            .putExtra(AutofillAuthActivity.EXTRA_PACKAGE_NAME, parsed.packageName)
            .putExtra(AutofillAuthActivity.EXTRA_WEB_DOMAIN,   parsed.webDomain)
            .putExtra(AutofillAuthActivity.EXTRA_USERNAME_ID,  parsed.usernameId)
            .putExtra(AutofillAuthActivity.EXTRA_PASSWORD_ID,  parsed.passwordId)

        val pending = PendingIntent.getActivity(
            this, key.hashCode(), authIntent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val locked = RemoteViews(packageName, R.layout.autofill_item).apply {
            setTextViewText(R.id.autofill_label, "Cleanthes \u2014 tap to fill")
        }

        val responseBuilder = FillResponse.Builder()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val presentations = Presentations.Builder()
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
            SaveInfo.Builder(
                SaveInfo.SAVE_DATA_TYPE_PASSWORD,
                arrayOf(parsed.usernameId, parsed.passwordId)
            ).build()
        )

        callback.onSuccess(responseBuilder.build())
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        val secretKey = sessionManager.getSessionKey()
        if (secretKey == null) { callback.onSuccess(); return }

        val contexts  = request.fillContexts
        val structure = contexts[contexts.size - 1].structure
        val parsed    = StructureParser.parse(structure)

        if (parsed.usernameId == null || parsed.passwordId == null) {
            callback.onSuccess(); return
        }

        val username = extractValue(structure, parsed.usernameId!!)
        val password = extractValue(structure, parsed.passwordId!!)
        val key      = parsed.webDomain ?: parsed.packageName ?: ""

        if (username != null && password != null) {
            try {
                @OptIn(DelicateCoroutinesApi::class)
                GlobalScope.launch(Dispatchers.IO) {
                    repository.addEntry(key, username, password, key, "Autofill", null, false, secretKey)
                }
            } catch (_: Exception) {}
        }
        callback.onSuccess()
    }

    private fun extractValue(structure: AssistStructure, target: AutofillId): String? {
        for (i in 0 until structure.windowNodeCount) {
            val v = findValue(structure.getWindowNodeAt(i).rootViewNode, target)
            if (v != null) return v
        }
        return null
    }

    private fun findValue(node: AssistStructure.ViewNode, target: AutofillId): String? {
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
}

