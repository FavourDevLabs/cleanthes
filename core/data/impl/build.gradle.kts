import java.util.Properties

plugins {
    id("com.android.library")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "dev.favourdevlabs.cleanthes.data.impl"
    compileSdk = 34

    defaultConfig {
        minSdk = 24

        val localProperties = Properties().apply {
            val file = rootProject.file("local.properties")
            if (file.exists()) load(file.inputStream())
        }

        buildConfigField(
            "String",
            "FAVICON_PROXY_API_KEY",
            "\"${localProperties.getProperty("FAVICON_PROXY_API_KEY", "")}\"",
        )
        buildConfigField(
            "String",
            "FAVICON_PROXY_BASE_URL",
            "\"${localProperties.getProperty("FAVICON_PROXY_BASE_URL", "")}\"",
        )
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin { jvmToolchain(17) }

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

dependencies {
    api(project(":core:data:api"))
    implementation(project(":core:domain"))
    implementation(project(":core:security"))
    implementation(project(":core:security:session:api"))

    val roomVersion = "2.7.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    implementation("com.google.dagger:hilt-android:2.59")
    ksp("com.google.dagger:hilt-android-compiler:2.59")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
}

