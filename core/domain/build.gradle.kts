plugins {
    id("com.android.library")
}

android {
    namespace = "dev.favourdevlabs.cleanthes.domain"
    compileSdk = 34

    defaultConfig { minSdk = 24 }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin { jvmToolchain(17) }
}

dependencies {
    // No :core:data, no :core:security, no Hilt, no Room
    // javax.crypto.SecretKey is part of the JVM — no import needed
}
