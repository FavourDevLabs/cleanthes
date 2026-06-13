# Cleanthes
> *"Guard it as Cleanthes guarded virtue."*

A zero-knowledge AES-256-GCM encrypted password vault for Android.  
No cloud. No backdoors. No second chances.

Named after Cleanthes of Assos (331–230 BC), Zeno's Stoic successor.
He worked nights as a water-carrier to fund his philosophy by day.
The inner citadel is yours alone.

---

## What makes it secure

Your master password never leaves your phone. It is never sent to a server.
It cannot be recovered by anyone, including the developer.

When you create a vault, this happens:

    Your password + random salt
            ↓
    310,000 rounds of PBKDF2 hashing
    (meets OWASP 2023 recommendations)
            ↓
    An AES-256 encryption key (lives in RAM only, never written to disk)

When an entry is saved:

    Your entry's password
            ↓
    AES-256-GCM encryption (unique random IV per entry)
            ↓
    Stored in Room database as ciphertext

Without the correct master password, the database is unreadable noise.

---

## Threat Model

**Cleanthes protects against:**
- Anyone with physical access to your device who does not know your master password
- Apps that read your storage
- Network-level attackers (no internet permission is declared)

**Cleanthes does not protect against:**
- A fully compromised OS or rooted device under attacker control
- Someone who already knows your master password
- Hardware-level forensics on an unlocked device

---

## Security Guarantees

- Nothing sensitive is written to disk in plaintext
- No internet permission — the app makes zero network calls
- No cloud sync, no analytics, no telemetry
- Screenshots disabled on all screens (`FLAG_SECURE`)
- Session auto-locks after 5 minutes of inactivity
- 5 failed attempts triggers a 30-second lockout
- Android backup disabled — vault excluded from phone backups

---

## Tech Stack

| Layer | Choice |
|---|---|
| Language | Kotlin |
| Architecture | MVVM — ViewModel, StateFlow, Coroutines |
| Database | Room (SQLite) with WAL mode |
| Encryption | AES-256-GCM via `javax.crypto` |
| Key Derivation | PBKDF2WithHmacSHA256 (310,000 iterations) |
| Secure Storage | EncryptedSharedPreferences + Android Keystore |
| Biometrics | BiometricPrompt API |
| Dependency Injection | Hilt |
| UI | Jetpack Compose + Material 3 |
| Autofill | Android Autofill Framework |

---

## Features

- Zero-knowledge vault — master password never leaves the device
- AES-256-GCM encryption with unique IV per entry
- TOTP authenticator support (scan QR or paste Base32 secret)
- Password strength meter (real-time, 5-segment)
- FORGE — built-in password generator with configurable complexity
- Biometric unlock (fingerprint)
- Android Autofill Service integration
- Category organisation with filter
- Priority entry marking
- Session auto-lock with manual lock option
- Swipe to delete with undo
- Live search by title or username
- Copy password or username to clipboard (auto-clears after 30 seconds)
- Show/hide password toggle

---

## Project Structure

Cleanthes is organized as a multi-module Gradle project. Dependencies flow
strictly downward — no module imports from a layer above it.

    ├── app/                        Application shell, UI, Hilt entry point
    │   ├── autofill/               Android Autofill Service
    │   └── ui/                     All screens and ViewModels
    │
    ├── core/
    │   ├── common/                 Shared utilities (ClipboardHelper, PasswordGenerator)
    │   ├── security/               Cryptography, key derivation, session management
    │   ├── data/                   Room database, DAO, repository
    │   └── domain/                 Use cases — business logic coordination
    │
    └── build.gradle.kts            Project-level Gradle configuration

Dependency graph:

    app → core:domain → core:data → core:security
                                  → core:common
              └───────────────────→ core:security

---

## Build and Run

**Requirements:** JDK 17+, Android SDK 34, device or emulator on API 24+

**Command line:**

    git clone https://github.com/FavourDevLabs/cleanthes.git
    cd cleanthes
    ./gradlew assembleDebug

Install the APK from `app/build/outputs/apk/debug/`

---

## The Name

Cleanthes of Assos (331–230 BC) succeeded Zeno as head of the Stoic school.
He is remembered not for volume but for discipline.

*"The willing are led by fate. The unwilling are dragged."*

---

## License

MIT — see LICENSE file

---

Built by FavourDevLabs — https://favourdevlabs.dev

