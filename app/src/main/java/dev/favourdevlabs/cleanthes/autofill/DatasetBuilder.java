package dev.favourdevlabs.cleanthes.autofill;

import android.content.Context;
import android.service.autofill.Dataset;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;
import android.widget.RemoteViews;

import dev.favourdevlabs.cleanthes.R;
import dev.favourdevlabs.cleanthes.data.entities.VaultEntry;

public class DatasetBuilder {

    public static Dataset build(
            Context context,
            AutofillId usernameId,
            AutofillId passwordId,
            VaultEntry entry) {

        RemoteViews view = new RemoteViews(context.getPackageName(), R.layout.autofill_item);
        view.setTextViewText(R.id.autofill_label, entry.getUsername());

        return new Dataset.Builder(view)
                .setValue(usernameId, AutofillValue.forText(entry.getUsername()), view)
                .setValue(passwordId, AutofillValue.forText(entry.getEncryptedPassword()), view)
                .build();
    }
}
