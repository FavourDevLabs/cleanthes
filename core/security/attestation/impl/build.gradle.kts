plugins {
    id("com.android.library")
}

android {
    namespace = "dev.favourdevlabs.cleanthes.attestation.impl"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
}

dependencies {
    implementation(project(":core:security:attestation:api"))
}

