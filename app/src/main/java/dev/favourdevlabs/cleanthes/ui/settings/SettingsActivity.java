package dev.favourdevlabs.cleanthes.ui.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.autofill.AutofillManager;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import dev.favourdevlabs.cleanthes.ui.base.AuthenticatedActivity;

import dev.favourdevlabs.cleanthes.R;

public class SettingsActivity extends AuthenticatedActivity {

    private static final String PREFS_NAME = "cleanthes_prefs";
    public static final String KEY_AUTO_LOCK = "auto_lock_minutes";
    public static final String KEY_CLIPBOARD = "clipboard_clear_seconds";

    private static final int[] LOCK_VALUES = { 1, 5, 15, -1 };
    private static final String[] LOCK_LABELS = { "1 min", "5 min", "15 min", "Never" };

    private static final int[] CLIP_VALUES = { 30, 60, -1 };
    private static final String[] CLIP_LABELS = { "30s", "60s", "Off" };

    private SharedPreferences prefs;
    private AutofillManager autofillManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        autofillManager = getSystemService(AutofillManager.class);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        bindAutoLock();
        bindClipboard();
        bindAutofill();
        bindVersion();
        bindLicenses();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindAutofill(); // refresh status after returning from system settings
    }

    // -------------------------------------------------------------------------

    private void bindAutoLock() {
        TextView valueView = findViewById(R.id.value_auto_lock);
        valueView.setText(labelForLock(prefs.getInt(KEY_AUTO_LOCK, 5)));

        findViewById(R.id.row_auto_lock).setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Auto-lock after")
                .setItems(LOCK_LABELS, (dialog, which) -> {
                    prefs.edit().putInt(KEY_AUTO_LOCK, LOCK_VALUES[which]).apply();
                    valueView.setText(LOCK_LABELS[which]);
                })
                .show());
    }

    private void bindClipboard() {
        TextView valueView = findViewById(R.id.value_clipboard);
        valueView.setText(labelForClip(prefs.getInt(KEY_CLIPBOARD, 30)));

        findViewById(R.id.row_clipboard).setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Clear clipboard after")
                .setItems(CLIP_LABELS, (dialog, which) -> {
                    prefs.edit().putInt(KEY_CLIPBOARD, CLIP_VALUES[which]).apply();
                    valueView.setText(CLIP_LABELS[which]);
                })
                .show());
    }

    private void bindAutofill() {
        TextView statusView = findViewById(R.id.autofill_status_text);
        boolean active = autofillManager.hasEnabledAutofillServices();

        if (active) {
            statusView.setText("Active ✓");
            statusView.setTextColor(getColor(R.color.cleanthes_success));
            findViewById(R.id.row_autofill).setOnClickListener(null);
        } else {
            statusView.setText("Enable ›");
            statusView.setTextColor(getColor(R.color.cleanthes_accent));
            findViewById(R.id.row_autofill).setOnClickListener(v -> {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            });
        }
    }

    private void bindVersion() {
        TextView versionView = findViewById(R.id.value_version);
        try {
            String versionName = getPackageManager()
                    .getPackageInfo(getPackageName(), 0).versionName;
            versionView.setText(versionName);
        } catch (Exception e) {
            versionView.setText("1.0.0");
        }
    }

    private void bindLicenses() {
        findViewById(R.id.row_licenses).setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Open-source libraries")
                .setMessage(
                        "ZXing Android Embedded\n"
                                + "Apache 2.0 License\n\n"
                                + "AndroidX Biometric\n"
                                + "Apache 2.0 License\n\n"
                                + "AndroidX Security Crypto\n"
                                + "Apache 2.0 License\n\n"
                                + "Google Material Components\n"
                                + "Apache 2.0 License")
                .setPositiveButton("Close", null)
                .show());
    }

    // -------------------------------------------------------------------------

    private String labelForLock(int minutes) {
        return minutes == -1 ? "Never" : minutes + " min";
    }

    private String labelForClip(int seconds) {
        return seconds == -1 ? "Off" : seconds + "s";
    }
}
