package dev.favourdevlabs.cleanthes.ui.addedit;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import dev.favourdevlabs.cleanthes.R;
import dev.favourdevlabs.cleanthes.data.entities.VaultEntry;
import dev.favourdevlabs.cleanthes.data.repository.VaultRepository;
import dev.favourdevlabs.cleanthes.security.OtpAuthParser;
import dev.favourdevlabs.cleanthes.security.TOTPGenerator;
import dev.favourdevlabs.cleanthes.ui.auth.SessionManager;
import dev.favourdevlabs.cleanthes.utils.PasswordGenerator;

import javax.crypto.SecretKey;

public class AddEditActivity extends AppCompatActivity {

    public static final String EXTRA_ENTRY_ID = "extra_entry_id";
    public static final long NO_ENTRY_ID = -1L;

    // Form views
    private TextView tvScreenTitle;
    private ImageButton btnBack;
    private Button btnSave;
    private EditText etTitle;
    private EditText etUsername;
    private EditText etPassword;
    private ImageButton btnTogglePassword;
    private Button btnGenerate;
    private View[] strengthSegments;
    private EditText etWebsite;
    private Spinner spinnerCategory;
    private EditText etNotes;
    private EditText etTotpSecret;
    private ImageButton btnScanTotp;
    private CheckBox checkBoxFavorite;
    private TextView tvError;
    private Button btnDelete;

    // State
    private boolean isEditMode = false;
    private long entryId = NO_ENTRY_ID;
    private VaultEntry existingEntry = null;
    private boolean passwordVisible = false;
    private VaultRepository repository;

    // TOTP metadata from QR scan or from a loaded entry.
    // Default to the RFC standard values — correct for ~99% of services.
    private String scannedTotpAlgorithm = "SHA1";
    private int scannedTotpDigits = 6;
    private int scannedTotpPeriod = 30;
    private String scannedTotpIssuer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit);
        repository = VaultRepository.getInstance(this);
        bindViews();
        setupCategorySpinner();
        determineMode();
        attachListeners();
    }

    private void bindViews() {
        tvScreenTitle = findViewById(R.id.addedit_tv_title);
        btnBack = findViewById(R.id.addedit_btn_back);
        btnSave = findViewById(R.id.addedit_btn_save);
        etTitle = findViewById(R.id.addedit_et_title);
        etUsername = findViewById(R.id.addedit_et_username);
        etPassword = findViewById(R.id.addedit_et_password);
        btnTogglePassword = findViewById(R.id.addedit_btn_toggle_password);
        btnGenerate = findViewById(R.id.addedit_btn_generate);
        etWebsite = findViewById(R.id.addedit_et_website);
        spinnerCategory = findViewById(R.id.addedit_spinner_category);
        etNotes = findViewById(R.id.addedit_et_notes);
        etTotpSecret = findViewById(R.id.addedit_et_totp_secret);
        btnScanTotp = findViewById(R.id.addedit_btn_scan_totp);
        checkBoxFavorite = findViewById(R.id.addedit_checkbox_favorite);
        tvError = findViewById(R.id.addedit_tv_error);
        btnDelete = findViewById(R.id.btn_delete);

        btnSave.setBackgroundTintList(
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.citadel_gold)));

        strengthSegments = new View[] {
                findViewById(R.id.addedit_seg1), findViewById(R.id.addedit_seg2),
                findViewById(R.id.addedit_seg3), findViewById(R.id.addedit_seg4),
                findViewById(R.id.addedit_seg5)
        };
    }

    private void setupCategorySpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.categories_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    private void determineMode() {
        entryId = getIntent().getLongExtra(EXTRA_ENTRY_ID, NO_ENTRY_ID);
        if (entryId != NO_ENTRY_ID) {
            isEditMode = true;
            tvScreenTitle.setText(getString(R.string.addedit_title_edit));
            btnDelete.setVisibility(View.VISIBLE);
            loadExistingEntry();
        } else {
            tvScreenTitle.setText(getString(R.string.addedit_title_new));
            btnDelete.setVisibility(View.GONE);
        }
    }

    private void loadExistingEntry() {
        SecretKey key = SessionManager.getSessionKey();
        if (key == null) {
            finish();
            return;
        }
        new Thread(() -> {
            try {
                VaultEntry entry = repository.getEntryById(entryId, key);
                runOnUiThread(() -> {
                    if (entry != null) {
                        existingEntry = entry;
                        populateFields(entry);
                    } else
                        finish();
                });
            } catch (Exception e) {
                runOnUiThread(this::finish);
            }
        }).start();
    }

    private void populateFields(VaultEntry entry) {
        etTitle.setText(entry.getTitle());
        etUsername.setText(entry.getUsername());
        etPassword.setText(entry.getEncryptedPassword());
        etWebsite.setText(entry.getWebsite() != null ? entry.getWebsite() : "");
        etNotes.setText(entry.getNotes() != null ? entry.getNotes() : "");
        etTotpSecret.setText(entry.getTotpSecret() != null ? entry.getTotpSecret() : "");
        checkBoxFavorite.setChecked(entry.isFavorite());

        // Restore TOTP metadata from the loaded entry
        scannedTotpAlgorithm = entry.getTotpAlgorithm();
        scannedTotpDigits = entry.getTotpDigits();
        scannedTotpPeriod = entry.getTotpPeriod();
        scannedTotpIssuer = entry.getTotpIssuer();

        ArrayAdapter adapter = (ArrayAdapter) spinnerCategory.getAdapter();
        int pos = adapter.getPosition(entry.getCategory());
        if (pos >= 0)
            spinnerCategory.setSelection(pos);
        updateStrengthBar(entry.getEncryptedPassword());
    }

    private void attachListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> attemptSave());
        btnDelete.setOnClickListener(v -> confirmDelete());
        btnTogglePassword.setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            togglePasswordVisibility(passwordVisible);
        });
        btnGenerate.setOnClickListener(v -> showGeneratorDialog());

        // Launch ZXing QR scanner
        btnScanTotp.setOnClickListener(v -> {
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
            integrator.setPrompt("Scan the 2FA QR code from your service's setup page");
            integrator.setBeepEnabled(false);
            integrator.setOrientationLocked(true);
            integrator.initiateScan();
        });

        etPassword.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateStrengthBar(s.toString());
                hideError();
            }
        });
    }

    /**
     * ZXing delivers its result here. We parse it as an otpauth:// URI,
     * extract all TOTP metadata, and populate the secret field automatically.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() != null) {
                handleQrResult(result.getContents());
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void handleQrResult(String rawContent) {
        try {
            OtpAuthParser.OtpAuthData parsed = OtpAuthParser.parse(rawContent);

            // Populate secret field
            etTotpSecret.setText(parsed.secret);

            // Store full metadata — needed for correct code generation
            scannedTotpAlgorithm = parsed.algorithm;
            scannedTotpDigits = parsed.digits;
            scannedTotpPeriod = parsed.period;
            scannedTotpIssuer = parsed.issuer;

            // Pre-fill entry title only when creating a new entry with an empty title
            if (!isEditMode && etTitle.getText().toString().trim().isEmpty()
                    && parsed.issuer != null) {
                etTitle.setText(parsed.issuer);
            }

            hideError();
            Toast.makeText(this, "Authenticator secret imported", Toast.LENGTH_SHORT).show();

        } catch (UnsupportedOperationException e) {
            showError("HOTP is not supported — only TOTP (time-based) codes.");
        } catch (Exception e) {
            showError("Invalid QR code — not a valid 2FA setup code.");
        }
    }

    private void attemptSave() {
        String title = etTitle.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String website = etWebsite.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();
        String notes = etNotes.getText().toString().trim();
        boolean fav = checkBoxFavorite.isChecked();
        String totpRaw = etTotpSecret.getText().toString()
                .trim().toUpperCase().replaceAll("\\s+", "");

        if (title.isEmpty()) {
            showError("Title is required");
            etTitle.requestFocus();
            return;
        }
        if (username.isEmpty()) {
            showError("Username or Email is required");
            etUsername.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            showError("Password is required");
            etPassword.requestFocus();
            return;
        }

        // Validate TOTP secret by generating a live code — catches bad Base32
        // immediately
        if (!totpRaw.isEmpty()) {
            try {
                TOTPGenerator.generate(totpRaw, scannedTotpDigits,
                        scannedTotpPeriod, scannedTotpAlgorithm);
            } catch (Exception e) {
                showError("Invalid authenticator secret — check the Base32 code.");
                etTotpSecret.requestFocus();
                return;
            }
        }

        SecretKey key = SessionManager.getSessionKey();
        if (key == null) {
            finish();
            return;
        }

        final String finalTotp = totpRaw.isEmpty() ? null : totpRaw;
        final String finalIssuer = finalTotp != null ? scannedTotpIssuer : null;
        final String finalAlgo = scannedTotpAlgorithm;
        final int finalDigits = scannedTotpDigits;
        final int finalPeriod = scannedTotpPeriod;

        setFormEnabled(false);

        new Thread(() -> {
            try {
                if (isEditMode && existingEntry != null) {
                    existingEntry.setTitle(title);
                    existingEntry.setUsername(username);
                    existingEntry.setWebsite(website.isEmpty() ? null : website);
                    existingEntry.setCategory(category);
                    existingEntry.setNotes(notes.isEmpty() ? null : notes);
                    existingEntry.setFavorite(fav);
                    existingEntry.setTotpSecret(finalTotp);
                    existingEntry.setTotpIssuer(finalIssuer);
                    existingEntry.setTotpAlgorithm(finalAlgo);
                    existingEntry.setTotpDigits(finalDigits);
                    existingEntry.setTotpPeriod(finalPeriod);
                    repository.updateEntry(existingEntry, password, key);
                } else {
                    repository.addEntry(
                            title, username, password,
                            website.isEmpty() ? null : website,
                            category,
                            notes.isEmpty() ? null : notes,
                            fav,
                            finalTotp, finalIssuer,
                            finalDigits, finalPeriod, finalAlgo,
                            key);
                }
                runOnUiThread(() -> {
                    setFormEnabled(true);
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setFormEnabled(true);
                    showError("Failed to save entry. Please try again.");
                });
            }
        }).start();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Entry")
                .setMessage("Delete \"" + etTitle.getText().toString() + "\"? This cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> performDelete())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performDelete() {
        if (!isEditMode || existingEntry == null)
            return;
        new Thread(() -> {
            try {
                repository.deleteEntry(existingEntry.getId());
                runOnUiThread(this::finish);
            } catch (Exception e) {
                runOnUiThread(() -> showError("Failed to delete entry."));
            }
        }).start();
    }

    private void showGeneratorDialog() {
        final int[] length = { PasswordGenerator.DEFAULT_LENGTH };
        final boolean[] uppercase = { true }, lowercase = { true },
                digits = { true }, special = { true };

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = dpToPx(20);
        layout.setPadding(pad, pad, pad, 0);
        layout.setBackgroundColor(getColor(R.color.cleanthes_surface));

        android.widget.LinearLayout lengthRow = new android.widget.LinearLayout(this);
        lengthRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        lengthRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView tvLen = new TextView(this);
        tvLen.setText("Length: " + length[0]);
        tvLen.setTextColor(getColor(R.color.citadel_gold));
        tvLen.setTextSize(15);
        tvLen.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button minus = makeGoldButton("−");
        Button plus = makeGoldButton("+");
        lengthRow.addView(tvLen);
        lengthRow.addView(minus);
        lengthRow.addView(plus);
        layout.addView(lengthRow);

        CheckBox cbUp = makeDialogCheckbox("Uppercase (A-Z)", true);
        CheckBox cbLo = makeDialogCheckbox("Lowercase (a-z)", true);
        CheckBox cbDi = makeDialogCheckbox("Digits (0-9)", true);
        CheckBox cbSp = makeDialogCheckbox("Special (!@#$...)", true);
        layout.addView(cbUp);
        layout.addView(cbLo);
        layout.addView(cbDi);
        layout.addView(cbSp);

        TextView tvPreview = new TextView(this);
        tvPreview.setTextColor(getColor(R.color.citadel_gold));
        tvPreview.setTextSize(14);
        tvPreview.setTypeface(android.graphics.Typeface.MONOSPACE);
        tvPreview.setPadding(0, dpToPx(12), 0, dpToPx(4));
        tvPreview.setText(PasswordGenerator.generate(length[0], true, true, true, true));
        layout.addView(tvPreview);

        Button btnRegen = new Button(this);
        btnRegen.setText("↻  REGENERATE");
        btnRegen.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                getColor(R.color.cleanthes_border)));
        btnRegen.setTextColor(getColor(R.color.citadel_gold));
        layout.addView(btnRegen);

        Runnable regen = () -> {
            try {
                tvPreview.setText(PasswordGenerator.generate(
                        length[0], uppercase[0], lowercase[0], digits[0], special[0]));
            } catch (IllegalArgumentException e) {
                tvPreview.setText("Select at least one category");
            }
        };

        minus.setOnClickListener(v -> {
            if (length[0] > 8) {
                length[0]--;
                tvLen.setText("Length: " + length[0]);
                regen.run();
            }
        });
        plus.setOnClickListener(v -> {
            if (length[0] < 32) {
                length[0]++;
                tvLen.setText("Length: " + length[0]);
                regen.run();
            }
        });
        cbUp.setOnCheckedChangeListener((b, c) -> {
            uppercase[0] = c;
            regen.run();
        });
        cbLo.setOnCheckedChangeListener((b, c) -> {
            lowercase[0] = c;
            regen.run();
        });
        cbDi.setOnCheckedChangeListener((b, c) -> {
            digits[0] = c;
            regen.run();
        });
        cbSp.setOnCheckedChangeListener((b, c) -> {
            special[0] = c;
            regen.run();
        });
        btnRegen.setOnClickListener(v -> regen.run());

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Forge a key")
                .setView(layout)
                .setPositiveButton("Commit to this", (dialog, which) -> {
                    String gen = tvPreview.getText().toString();
                    if (!gen.equals("Select at least one category")) {
                        etPassword.setText(gen);
                        updateStrengthBar(gen);
                        if (!passwordVisible) {
                            passwordVisible = true;
                            togglePasswordVisibility(true);
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private Button makeGoldButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(16);
        b.setTextColor(getColor(R.color.cleanthes_black));
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                getColor(R.color.citadel_gold)));
        return b;
    }

    private CheckBox makeDialogCheckbox(String label, boolean checked) {
        CheckBox cb = new CheckBox(this);
        cb.setText(label);
        cb.setChecked(checked);
        cb.setTextColor(getColor(R.color.cleanthes_text_secondary));
        cb.setButtonTintList(android.content.res.ColorStateList.valueOf(
                getColor(R.color.citadel_gold)));
        return cb;
    }

    private void updateStrengthBar(String password) {
        int score = PasswordGenerator.evaluateStrength(password);
        int[] colorRes = {
                R.color.cleanthes_strength_empty, R.color.cleanthes_strength_very_weak,
                R.color.cleanthes_strength_weak, R.color.cleanthes_strength_fair,
                R.color.cleanthes_strength_strong, R.color.cleanthes_strength_very_strong
        };
        int active = ContextCompat.getColor(this, colorRes[score]);
        int empty = ContextCompat.getColor(this, R.color.cleanthes_strength_empty);
        for (int i = 0; i < strengthSegments.length; i++)
            strengthSegments[i].setBackgroundColor(i < score ? active : empty);
    }

    private void togglePasswordVisibility(boolean visible) {
        int type = visible
                ? android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                : android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD;
        etPassword.setInputType(type);
        etPassword.setSelection(etPassword.getText().length());
        btnTogglePassword.setImageResource(visible ? R.drawable.ic_eye_on : R.drawable.ic_eye_off);
    }

    private void setFormEnabled(boolean enabled) {
        btnSave.setEnabled(enabled);
        btnSave.setAlpha(enabled ? 1f : 0.5f);
        btnSave.setBackgroundTintList(
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.citadel_gold)));
        etTitle.setEnabled(enabled);
        etPassword.setEnabled(enabled);
        etUsername.setEnabled(enabled);
    }

    private void showError(String msg) {
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        tvError.setVisibility(View.GONE);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    }
}
