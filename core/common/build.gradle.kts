plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.neon.ascent.core.common"
    compileSdk = 35
    defaultConfig {
        minSdk = 31
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
}
