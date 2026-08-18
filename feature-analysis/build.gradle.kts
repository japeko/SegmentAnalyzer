plugins {
    alias(libs.plugins.android.library)
}

// Not implemented yet. Reserved for the Ride Analysis screen (map + charts + splits).
android {
    namespace = "com.segmentanalyzer.feature.analysis"
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
    implementation(project(":core"))
}
