import re

path = "app/src/main/java/dev/favourdevlabs/cleanthes/ui/auth/SetupActivity.kt"
with open(path, "r") as f:
    content = f.read()

# 1. Add NavigateToLogin handler into the when block
old_when = """                    when (event) {
                        SetupNavEvent.NavigateToHome -> {
                            startActivity(
                                Intent(this@SetupActivity, HomeActivity::class.java)
                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
                            )
                            finish()
                        }
                        is SetupNavEvent.TriggerBiometricEnrollment -> triggerBiometricEnrollment(event.cipher)
                    }"""

new_when = """                    when (event) {
                        SetupNavEvent.NavigateToHome -> {
                            startActivity(
                                Intent(this@SetupActivity, HomeActivity::class.java)
                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
                            )
                            finish()
                        }
                        SetupNavEvent.NavigateToLogin -> {
                            startActivity(
                                Intent(this@SetupActivity, LoginActivity::class.java)
                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
                            )
                            finish()
                        }
                        is SetupNavEvent.TriggerBiometricEnrollment -> triggerBiometricEnrollment(event.cipher)
                    }"""

content = content.replace(old_when, new_when)

# 2. Replace onSplashComplete() — no more prefs read
old_splash = """        splashHandler.postDelayed(::onSplashComplete, 2000)
    }

    override fun onDestroy() {
        super.onDestroy()
        splashHandler.removeCallbacksAndMessages(null)
    }

    private fun onSplashComplete() {
        try {
            if (getEncryptedPrefs().getBoolean(KEY_VAULT_EXISTS, false)) {
                splashDone = true
                startActivity(
                    Intent(this, LoginActivity::class.java)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
                )
                finish()
                return
            }
        } catch (_: Exception) {
        }

        showContent = true
        splashDone = true
    }"""

new_splash = """        splashHandler.postDelayed(::onSplashComplete, 2000)
        viewModel.checkVaultExists()
    }

    override fun onDestroy() {
        super.onDestroy()
        splashHandler.removeCallbacksAndMessages(null)
    }

    private fun onSplashComplete() {
        showContent = true
        splashDone = true
    }"""

content = content.replace(old_splash, new_splash)

# 3. Remove getEncryptedPrefs()
old_prefs = """
    // Routing-only — ViewModel owns the write path
    private fun getEncryptedPrefs() =
        EncryptedSharedPreferences.create(
            this,
            PREFS_NAME,
            MasterKey.Builder(this).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )"""

content = content.replace(old_prefs, "")

with open(path, "w") as f:
    f.write(content)

print("Done")
