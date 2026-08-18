plugins {
    alias(libs.plugins.android.library)
}

// Not implemented yet. Reserved for Garmin Connect / Strava account sign-in.
android {
    namespace = "com.segmentanalyzer.feature.auth"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":common"))
}
