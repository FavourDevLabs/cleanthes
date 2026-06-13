package dev.favourdevlabs.cleanthes.autofill

import android.content.Context
import android.os.Build
import android.service.autofill.Dataset
import android.service.autofill.Presentations
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import dev.favourdevlabs.cleanthes.R
import dev.favourdevlabs.cleanthes.data.entities.VaultEntry

object DatasetBuilder {

    fun build(
        context: Context,
        usernameId: AutofillId,
        passwordId: AutofillId,
        entry: VaultEntry
    ): Dataset {
        val view = RemoteViews(context.packageName, R.layout.autofill_item).apply {
            setTextViewText(R.id.autofill_label, entry.username)
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val presentations = Presentations.Builder()
                .setMenuPresentation(view)
                .build()
            @Suppress("DEPRECATION")
            Dataset.Builder(presentations)
                .setValue(usernameId, AutofillValue.forText(entry.username))
                .setValue(passwordId, AutofillValue.forText(entry.encryptedPassword))
                .build()
        } else {
            @Suppress("DEPRECATION")
            Dataset.Builder(view)
                .setValue(usernameId, AutofillValue.forText(entry.username), view)
                .setValue(passwordId, AutofillValue.forText(entry.encryptedPassword), view)
                .build()
        }
    }
}

