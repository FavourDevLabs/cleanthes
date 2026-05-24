package dev.favourdevlabs.cleanthes.autofill;

import android.content.Intent;
import android.os.Bundle;
import android.service.autofill.Dataset;
import android.service.autofill.FillResponse;
import android.view.WindowManager;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillManager;
import android.view.autofill.AutofillValue;
import android.widget.RemoteViews;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import dev.favourdevlabs.cleanthes.R;
import dev.favourdevlabs.cleanthes.data.entities.VaultEntry;
import dev.favourdevlabs.cleanthes.data.repository.VaultRepository;
import dev.favourdevlabs.cleanthes.ui.auth.SessionManager;

import java.util.ArrayList;
import java.util.List;
import javax.crypto.SecretKey;

public class AutofillAuthActivity extends AppCompatActivity {

    public static final String EXTRA_PACKAGE_NAME = "pkg";
    public static final String EXTRA_WEB_DOMAIN   = "domain";
    public static final String EXTRA_USERNAME_ID  = "uid";
    public static final String EXTRA_PASSWORD_ID  = "pid";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_autofill_auth);

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
        );

        // Vault is locked — user must open Cleanthes and unlock first
        if (!SessionManager.isUnlocked()) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        prompt();
    }

    private void prompt() {
        new BiometricPrompt(this, ContextCompat.getMainExecutor(this),
                new BiometricPrompt.AuthenticationCallback() {

                    @Override
                    public void onAuthenticationSucceeded(
                            @NonNull BiometricPrompt.AuthenticationResult result) {
                        deliver();
                    }

                    @Override
                    public void onAuthenticationFailed() {}

                    @Override
                    public void onAuthenticationError(
                            int code, @NonNull CharSequence msg) {
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                }
        ).authenticate(new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Cleanthes")
                .setSubtitle("Authenticate to fill")
                .setNegativeButtonText("Cancel")
                .build());
    }

    private void deliver() {
        SecretKey secretKey = SessionManager.getSessionKey();
        if (secretKey == null) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        Intent in = getIntent();
        AutofillId usernameId = in.getParcelableExtra(EXTRA_USERNAME_ID);
        AutofillId passwordId = in.getParcelableExtra(EXTRA_PASSWORD_ID);
        String packageName    = in.getStringExtra(EXTRA_PACKAGE_NAME);
        String webDomain      = in.getStringExtra(EXTRA_WEB_DOMAIN);
        String lookupKey      = webDomain != null ? webDomain : packageName;

        try {
            List<VaultEntry> all = VaultRepository.getInstance(this).getAllEntries(secretKey);
            List<VaultEntry> matches = filter(all, lookupKey);

            if (matches.isEmpty()) {
                setResult(RESULT_CANCELED);
                finish();
                return;
            }

            FillResponse.Builder response = new FillResponse.Builder();
            for (VaultEntry entry : matches) {
                RemoteViews view = new RemoteViews(getPackageName(), R.layout.autofill_item);
                view.setTextViewText(R.id.autofill_label, entry.getUsername());

                // After getAllEntries(), encryptedPassword field holds the
                // decrypted plain password — VaultRepository.decryptEntry()
                // overwrites the field in place.
                response.addDataset(new Dataset.Builder(view)
                        .setValue(usernameId,
                                AutofillValue.forText(entry.getUsername()), view)
                        .setValue(passwordId,
                                AutofillValue.forText(entry.getEncryptedPassword()), view)
                        .build());
            }

            SessionManager.refreshSession();

            Intent out = new Intent();
            out.putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, response.build());
            setResult(RESULT_OK, out);

        } catch (Exception e) {
            setResult(RESULT_CANCELED);
        }

        finish();
    }

    private List<VaultEntry> filter(List<VaultEntry> entries, String key) {
        List<VaultEntry> out = new ArrayList<>();
        if (key == null || key.isEmpty()) return out;
        String lower = key.toLowerCase();
        for (VaultEntry e : entries) {
            String website = e.getWebsite() != null ? e.getWebsite().toLowerCase() : "";
            String title   = e.getTitle()   != null ? e.getTitle().toLowerCase()   : "";
            if (website.contains(lower) || lower.contains(website)
                    || title.contains(lower)) {
                out.add(e);
            }
        }
        return out;
    }
}
