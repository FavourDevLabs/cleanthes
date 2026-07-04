# Cleanthes

> *"Guard it as Cleanthes guarded virtue."*

Zero-knowledge AES-256-GCM password vault for Android.
No cloud. No backdoors. No second chances.

Named after Cleanthes of Assos (331–230 BC), Stoic successor to Zeno.

---

## Security Architecture

### Envelope Encryption

```
Master password + random salt
        ↓
PBKDF2-HMAC-SHA256 — 310,000 iterations (OWASP 2023)
        ↓
Password-derived wrapping key
        ↓  wraps
Random 256-bit vault key  ←── generated once at setup
        ↓  encrypts
Each vault entry (AES-256-GCM, unique 96-bit IV per entry)
```

Biometric path — hardware-bound, not just a gate:

```
Biometric authentication (TEE/StrongBox verified)
        ↓
Android Keystore AES key — setUserAuthenticationRequired(true)
Key never leaves secure hardware. Crypto-bound to biometric result.
        ↓  unwraps
Vault key → decrypt entries
```

Both paths independently arrive at the same vault key.
Changing your password does not re-encrypt the vault — only re-wraps the key.

### Cryptographic Primitives

| Operation             | Algorithm           | Parameters                         |
|-----------------------|---------------------|------------------------------------|
| Key derivation        | PBKDF2-HMAC-SHA256  | 310,000 iterations, 256-bit output |
| Entry encryption      | AES-256-GCM         | 128-bit tag, 96-bit random IV      |
| Biometric binding     | Keystore AES-GCM    | UserAuthenticationRequired(true)   |
| Password verification | PBKDF2 + isEqual    | Constant-time comparison           |
| Key wrapping          | AES-GCM             | IV prepended to ciphertext         |

### Threat Model

**Protected against:**
- Physical device access without master password
- Storage-reading apps — database is unreadable without the vault key
- Network attackers — no `INTERNET` permission declared
- Backup exfiltration — `android:allowBackup="false"`

**Not protected against:**
- Fully compromised OS or rooted device under attacker control
- Attacker who already knows the master password
- Hardware forensics on an active, unlocked device

### Session Security

- `FLAG_SECURE` on all screens — no screenshots, no Recents thumbnail
- Session key lives in RAM only — never written to disk
- Auto-locks after 5 minutes of inactivity
- 5 failed attempts triggers 30-second lockout
- Manual lock clears key immediately from memory

---

## Architecture

Structured after the Proton Pass open-source Android architecture.
Strict `api` / `impl` / `fakes` module triads with enforced compile-time boundaries.
Feature modules cannot import implementation classes — verified at build time.

### Module Graph

```
:app                               Hilt entry point. Application class only.
 │
 ├── :feature:auth                 SetupActivity, LoginActivity + ViewModels
 ├── :feature:home                 HomeActivity + HomeViewModel
 ├── :feature:addedit              AddEditActivity + AddEditViewModel
 ├── :feature:detail               DetailActivity + DetailViewModel
 ├── :feature:settings             SettingsActivity
 ├── :feature:autofill             AutofillService, DatasetBuilder, StructureParser
 │
 ├── :core:ui                      AuthenticatedActivity, theme, shared Composables
 │
 ├── :core:domain                  VaultItem, use case interfaces,
 │                                 TOTPGenerator, OtpAuthParser — pure JVM, no Android
 │
 ├── :core:data:api                VaultRepository interface, use case contracts
 ├── :core:data:impl               VaultRepositoryImpl, Room, DAOs, crypto calls
 ├── :core:data:fakes              In-memory fakes for unit tests
 │
 ├── :core:security                CryptoManager, KeyDerivation, KeystoreManager,
 │                                 BiometricHelper
 ├── :core:security:session:api    SessionManager interface (StateFlow-based)
 ├── :core:security:session:impl   SessionManagerImpl
 │
 └── :core:common                  PasswordGenerator, ClipboardHelper, DateUtils
```

### api / impl / fakes Triad

```
:core:data:api    — interfaces and domain models only.
                    Feature modules depend on this. Never see the impl.
:core:data:impl   — Room, crypto, SharedPreferences writes.
                    Only :app depends on this, for Hilt wiring.
:core:data:fakes  — in-memory stubs. Unit tests run on JVM, no emulator needed.
```

ViewModel tests run in milliseconds on bare JVM:

```kotlin
val fake = FakeSaveVaultEntry()
val vm = AddEditViewModel(
    getVaultEntry    = FakeGetVaultEntry().apply { result = testItem },
    saveVaultEntry   = fake,
    deleteVaultEntry = FakeDeleteVaultEntry(),
    sessionManager   = FakeSessionManager().apply { setKey(testKey) },
)
vm.attemptSave()
assertEquals(1, fake.callCount())
```

### Dependency Rules

```
:feature:*        →  :core:data:api, :core:domain, :core:ui
:core:data:impl   →  :core:data:api, :core:domain, :core:security
:core:data:api    →  :core:domain
:core:domain      →  nothing (pure JVM — no Android SDK)
```

---

## Tech Stack

| Layer                | Choice                                        |
|----------------------|-----------------------------------------------|
| Language             | Kotlin                                        |
| Architecture         | MVVM — ViewModel, StateFlow, Coroutines       |
| Module structure     | Multi-module, api/impl/fakes triad            |
| Database             | Room (SQLite) with WAL mode                   |
| Encryption           | AES-256-GCM via javax.crypto                  |
| Key derivation       | PBKDF2-HMAC-SHA256, 310,000 iterations        |
| Secure storage       | EncryptedSharedPreferences + Android Keystore |
| Biometrics           | BiometricPrompt with CryptoObject binding     |
| Dependency injection | Hilt — @Binds interface → impl wiring         |
| UI                   | Jetpack Compose + Material 3                  |
| Autofill             | Android Autofill Framework                    |
| TOTP                 | RFC 6238 — pure JVM, no third-party library   |

---

## Features

- Zero-knowledge vault — master password never leaves the device
- AES-256-GCM encryption with unique IV per entry
- Envelope encryption — vault key wrapped under password + hardware key
- Biometric unlock with hardware-backed crypto binding (not just a gate)
- TOTP authenticator — scan QR code or paste Base32 secret
- Password strength meter — real-time, 5-segment
- Built-in password generator — configurable length and character sets
- Android Autofill Service integration
- Category organisation with filter
- Priority entry marking
- Session auto-lock (5 minutes) with manual lock
- Swipe-to-delete with undo
- Live search by title or username
- Clipboard auto-clear after 30 seconds

---

## Build

Requirements: JDK 17+, Android SDK 34, API 24+ device or emulator.

```bash
git clone https://github.com/FavourDevLabs/cleanthes.git
cd cleanthes
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

---

## The Name

Cleanthes of Assos (331–230 BC) succeeded Zeno as head of the Stoic school.
He worked nights as a water-carrier to fund his philosophy by day.
He is remembered not for volume but for discipline.

*"The willing are led by fate. The unwilling are dragged."*

---

## License

MIT — see LICENSE file.

---

Built by FavourDevLabs

