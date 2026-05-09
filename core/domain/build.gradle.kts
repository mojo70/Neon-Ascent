plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.neon.ascent.core.domain"
    compileSdk = 35
    defaultConfig {
        minSdk = 31
    }
}

dependencies {
    implementation(project(":core:common"))
}
