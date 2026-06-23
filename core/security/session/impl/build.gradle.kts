plugins {
    id("com.android.library")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "dev.favourdevlabs.cleanthes.security.session.impl"
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
    implementation(project(":core:security:session:api"))

    implementation("com.google.dagger:hilt-android:2.59")
    ksp("com.google.dagger:hilt-android-compiler:2.59")
}
