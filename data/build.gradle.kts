import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
}

// Strava app credentials, kept out of version control. Register a free app at
// strava.com/settings/api and add STRAVA_CLIENT_ID / STRAVA_CLIENT_SECRET to local.properties.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.segmentanalyzer.data"
    compileSdk = 37

    defaultConfig {
        minSdk = 26

        buildConfigField("String", "STRAVA_CLIENT_ID", "\"${localProperties.getProperty("STRAVA_CLIENT_ID", "")}\"")
        buildConfigField(
            "String",
            "STRAVA_CLIENT_SECRET",
            "\"${localProperties.getProperty("STRAVA_CLIENT_SECRET", "")}\"",
        )
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":common"))

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.okhttp.core)
    implementation(libs.androidx.security.crypto)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.garmin.fit)
    implementation(libs.mlkit.genai.prompt)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
}
